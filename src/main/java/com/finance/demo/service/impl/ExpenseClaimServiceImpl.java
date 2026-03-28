package com.finance.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.demo.entity.*;
import com.finance.demo.enums.ClaimStatus;
import com.finance.demo.enums.UserRole;
import com.finance.demo.mapper.*;
import com.finance.demo.service.DepartmentBudgetService;
import com.finance.demo.service.ExpenseClaimService;
import com.finance.demo.service.risk.RiskDetectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报销单服务实现
 * 核心业务规则：
 * 1. 防止自己审批自己
 * 2. IT管理员不能参与审批
 * 3. 部门经理只能审批本部门的报销单
 * 4. 完整记录审计日志（时间痕迹和修改痕迹）
 * 5. 提交时自动触发风险检测
 */
@Slf4j
@Service
public class ExpenseClaimServiceImpl extends ServiceImpl<ExpenseClaimMapper, ExpenseClaim> implements ExpenseClaimService {

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private RiskDetectionService riskDetectionService;

    @Autowired
    private DepartmentBudgetService departmentBudgetService;

    @Autowired
    private RiskAlertMapper riskAlertMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean submitClaim(ExpenseClaim claim, Integer userRole) {
        // 生成报销单编号
        if (claim.getClaimNo() == null || claim.getClaimNo().isEmpty()) {
            claim.setClaimNo(generateClaimNo());
        }

        // 【核心逻辑】经理提交报销单直接进入财务审批，跳过自己审批自己
        if (userRole != null && userRole.equals(UserRole.MANAGER.getCode())) {
            claim.setStatus(ClaimStatus.PENDING_FINANCE.getCode());
        } else {
            // 普通员工提交，进入部门经理审批
            claim.setStatus(ClaimStatus.PENDING_MANAGER.getCode());
        }

        boolean saved = this.save(claim);

        // 【新增】提交时冻结预算
        if (saved && claim.getId() != null) {
            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(claim.getAmount()).divide(new java.math.BigDecimal(100));
                Integer year = java.time.Year.now().getValue();
                log.info("提交报销单，冻结预算: claimId={}, deptId={}, amount={}, year={}", claim.getId(), claim.getDeptId(), amount, year);
                departmentBudgetService.freezeBudget(claim.getDeptId(), amount, year);
            } catch (Exception e) {
                log.error("预算冻结失败, claimId={}, error={}", claim.getId(), e.getMessage(), e);
            }
        }

        // 【修改为同步】风险检测在事务内同步执行
        if (saved && claim.getId() != null) {
            try {
                riskDetectionService.detectRisk(claim.getId());
            } catch (Exception e) {
                log.warn("风险检测失败, claimId={}", claim.getId(), e);
            }
        }

        return saved;
    }

    /**
     * 生成报销单编号
     * 格式：BX + 年月日 + 4位序号，如 BX20260325001
     */
    private String generateClaimNo() {
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询今天已有的最大序号
        long count = this.count(new QueryWrapper<ExpenseClaim>()
                .likeRight("claim_no", "BX" + dateStr));
        return String.format("BX%s%03d", dateStr, count + 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveClaim(Long claimId, boolean approved, String comment, Integer operatorId) {
        // 1. 获取报销单
        ExpenseClaim claim = this.getById(claimId);
        if (claim == null) {
            throw new RuntimeException("报销单不存在");
        }

        // 2. 获取操作人信息
        SysUser operator = userMapper.selectById(operatorId);
        if (operator == null) {
            throw new RuntimeException("操作人不存在");
        }

        // 3. 【核心】防止自己审批自己
        if (claim.getUserId().equals(operatorId)) {
            throw new RuntimeException("不能审批自己的报销单");
        }

        // 4. 【核心】IT管理员禁止审批
        if (operator.getRole().equals(UserRole.ADMIN.getCode())) {
            throw new RuntimeException("IT管理员不能参与审批流程");
        }

        // 5. 记录变更前状态
        int beforeStatus = claim.getStatus();
        String beforeValue = toJson(claim);

        // 6. 根据角色执行不同的审批逻辑
        int afterStatus;
        int approvalStage;

        if (operator.getRole().equals(UserRole.MANAGER.getCode())) {
            // 部门经理审批逻辑
            approvalStage = 1;

            // 检查是否是本部门的报销单
            Department dept = departmentMapper.selectById(claim.getDeptId());
            if (dept == null || !operatorId.equals(dept.getManagerId())) {
                throw new RuntimeException("只能审批本部门的报销单");
            }

            // 检查状态是否允许部门经理审批
            if (!ClaimStatus.fromCode(claim.getStatus()).canManagerApprove()) {
                throw new RuntimeException("报销单状态不允许部门经理审批");
            }

            afterStatus = approved ? ClaimStatus.PENDING_FINANCE.getCode() : ClaimStatus.MANAGER_REJECTED.getCode();

            // 【新增】经理审批时处理预算
            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(claim.getAmount()).divide(new java.math.BigDecimal(100));
                Integer year = java.time.Year.now().getValue();
                if (approved) {
                    // 审批通过：从冻结转为已使用 (move frozen -> used)
                    log.info("经理审批通过，扣减预算: claimId={}, deptId={}, amount={}", claim.getId(), claim.getDeptId(), amount);
                    departmentBudgetService.deductBudget(claim.getDeptId(), amount, year);
                } else {
                    // 审批驳回：释放冻结
                    log.info("经理审批驳回，释放预算: claimId={}, deptId={}, amount={}", claim.getId(), claim.getDeptId(), amount);
                    departmentBudgetService.releaseFrozen(claim.getDeptId(), amount, year);
                }
            } catch (Exception e) {
                log.error("经理审批时预算处理异常, claimId={}, error={}", claim.getId(), e.getMessage(), e);
            }

        } else if (operator.getRole().equals(UserRole.FINANCE.getCode())) {
            // 财务审计逻辑
            approvalStage = 2;

            // 检查状态是否允许财务审计
            if (!ClaimStatus.fromCode(claim.getStatus()).canFinanceAudit()) {
                throw new RuntimeException("报销单未通过部门经理审批，不能进行财务审计");
            }

            afterStatus = approved ? ClaimStatus.FINANCE_APPROVED.getCode() : ClaimStatus.FINANCE_REJECTED.getCode();

            // 【新增】财务审批时处理预算（处理经理提交跳过审批的情况）
            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(claim.getAmount()).divide(new java.math.BigDecimal(100));
                Integer year = java.time.Year.now().getValue();
                if (approved) {
                    // 审批通过：从冻结转为已使用 (如果经理已处理则frozen为0不重复扣减)
                    log.info("财务审批通过，扣减预算: claimId={}, deptId={}, amount={}", claim.getId(), claim.getDeptId(), amount);
                    departmentBudgetService.deductBudget(claim.getDeptId(), amount, year);
                } else {
                    // 审批驳回：释放冻结（如果经理已处理则frozen为0不重复释放）
                    log.info("财务审批驳回，释放预算: claimId={}, deptId={}, amount={}", claim.getId(), claim.getDeptId(), amount);
                    departmentBudgetService.releaseFrozen(claim.getDeptId(), amount, year);
                }
            } catch (Exception e) {
                log.error("财务审批时预算处理异常, claimId={}, error={}", claim.getId(), e.getMessage(), e);
            }

            // 【新增】财务审批完成后，清除该报销单的所有风险预警
            try {
                riskAlertMapper.delete(new QueryWrapper<RiskAlert>()
                    .eq("claim_id", claimId));
            } catch (Exception e) {
                log.error("清除风险预警失败, claimId={}, error={}", claimId, e.getMessage());
            }

        } else {
            throw new RuntimeException("无审批权限");
        }

        // 7. 更新报销单状态
        claim.setStatus(afterStatus);
        boolean updated = this.updateById(claim);

        if (updated) {
            // 8. 记录审批记录
            ApprovalRecord record = new ApprovalRecord();
            record.setClaimId(claimId.intValue());
            record.setApproverId(operatorId);
            record.setApprovalStage(approvalStage);
            record.setBeforeStatus(beforeStatus);
            record.setAfterStatus(afterStatus);
            record.setAction(approved ? "APPROVE" : "REJECT");
            record.setComment(comment);
            approvalRecordMapper.insert(record);

            // 9. 记录审计日志（完整记录变更前后值）
            AuditLog log = new AuditLog();
            log.setClaimId(claim.getId());
            log.setOperatorId(operatorId);
            log.setActionType(approved ? "APPROVE" : "REJECT");
            log.setAction(String.format("【%s】审批%s | 备注：%s",
                    operator.getRole().equals(UserRole.MANAGER.getCode()) ? "部门经理" : "财务审计",
                    approved ? "通过" : "驳回",
                    comment));
            log.setBeforeValue(beforeValue);
            log.setAfterValue(toJson(claim));
            auditLogMapper.insert(log);
        }

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Deprecated
    public boolean auditClaim(Long claimId, Integer status, String comment, Integer operatorId) {
        // 兼容旧方法，转发到新方法
        boolean approved = status == ClaimStatus.FINANCE_APPROVED.getCode() || status == ClaimStatus.PENDING_FINANCE.getCode();
        return approveClaim(claimId, approved, comment, operatorId);
    }

    @Override
    public List<ExpenseClaim> getHighRiskClaims() {
        return baseMapper.selectWeekendClaims();
    }

    @Override
    public List<ExpenseClaim> getPendingClaims(Integer operatorId) {
        SysUser operator = userMapper.selectById(operatorId);
        if (operator == null) {
            return List.of();
        }

        QueryWrapper<ExpenseClaim> wrapper = new QueryWrapper<>();

        if (operator.getRole().equals(UserRole.MANAGER.getCode())) {
            // 部门经理：查看本部门待审批的报销单，排除自己的
            Department dept = departmentMapper.selectById(operator.getDeptId());
            if (dept != null && operatorId.equals(dept.getManagerId())) {
                wrapper.eq("dept_id", operator.getDeptId())
                       .eq("status", ClaimStatus.PENDING_MANAGER.getCode())
                       .ne("user_id", operatorId);  // 排除自己的报销单
            }
        } else if (operator.getRole().equals(UserRole.FINANCE.getCode())) {
            // 财务：查看所有待财务审计的报销单
            wrapper.eq("status", ClaimStatus.PENDING_FINANCE.getCode());
        }

        return this.list(wrapper);
    }

    @Override
    public List<ExpenseClaim> getMyClaims(Integer userId) {
        QueryWrapper<ExpenseClaim> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("created_at");
        return this.list(wrapper);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}