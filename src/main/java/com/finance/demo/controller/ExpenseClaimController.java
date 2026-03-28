package com.finance.demo.controller;

import com.finance.demo.common.Result;
import com.finance.demo.entity.*;
import com.finance.demo.enums.ClaimStatus;
import com.finance.demo.enums.UserRole;
import com.finance.demo.mapper.*;
import com.finance.demo.service.DepartmentBudgetService;
import com.finance.demo.service.ExpenseClaimService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 报销单控制器
 * 权限说明：
 * - 普通员工：只能查看和提交自己的报销单
 * - 部门经理：可以查看本部门报销单，审批待审单据
 * - 财务内审：可以查看所有报销单，进行最终审计
 * - IT管理员：可以查看所有报销单，但不能审批
 */
@RestController
@RequestMapping("/api/claims")
@CrossOrigin
public class ExpenseClaimController {

    @Autowired
    private ExpenseClaimService expenseClaimService;

    @Autowired
    private DepartmentBudgetService departmentBudgetService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private RiskAlertMapper riskAlertMapper;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 获取报销单列表（按角色过滤）
     * - 员工：只看自己的
     * - 经理：看本部门的
     * - 财务：看所有，但不包括待经理审批的（状态>=2）
     * - 管理员：看所有
     */
    @GetMapping("/list")
    public Result<List<ExpenseClaim>> list(HttpServletRequest request) {
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("请先登录");
        }

        SysUser currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            return Result.error("用户不存在");
        }

        List<ExpenseClaim> list;

        if (currentUser.getRole().equals(UserRole.EMPLOYEE.getCode())) {
            // 普通员工：只能查看自己的报销单
            list = expenseClaimService.getMyClaims(currentUserId);
        } else if (currentUser.getRole().equals(UserRole.MANAGER.getCode())) {
            // 部门经理：查看本部门的报销单
            QueryWrapper<ExpenseClaim> wrapper = new QueryWrapper<>();
            wrapper.eq("dept_id", currentUser.getDeptId()).orderByDesc("created_at");
            list = expenseClaimService.list(wrapper);
        } else if (currentUser.getRole().equals(UserRole.FINANCE.getCode())) {
            // 财务：查看所有，但不包括待经理审批的（状态>=2）
            QueryWrapper<ExpenseClaim> wrapper = new QueryWrapper<>();
            wrapper.ge("status", 2).orderByDesc("created_at");  // 状态>=2
            list = expenseClaimService.list(wrapper);
        } else {
            // 管理员：查看所有
            list = expenseClaimService.list();
        }

        return Result.success(list);
    }

    /**
     * 获取待审批的报销单（部门经理和财务使用）
     */
    @GetMapping("/pending")
    public Result<List<ExpenseClaim>> getPending(HttpServletRequest request) {
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("请先登录");
        }

        SysUser currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            return Result.error("用户不存在");
        }

        // 只有经理和财务可以查看待审批列表
        if (!currentUser.getRole().equals(UserRole.MANAGER.getCode()) &&
            !currentUser.getRole().equals(UserRole.FINANCE.getCode())) {
            return Result.error("无权限");
        }

        List<ExpenseClaim> list = expenseClaimService.getPendingClaims(currentUserId);
        return Result.success(list);
    }

    /**
     * 提交报销单（员工操作）
     */
    @PostMapping("/submit")
    public Result submit(@RequestBody ExpenseClaim claim, HttpServletRequest request) {
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("请先登录");
        }

        SysUser currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            return Result.error("用户不存在");
        }

        // IT管理员不能提交报销单
        if (currentUser.getRole().equals(UserRole.ADMIN.getCode())) {
            return Result.error("IT管理员不能参与经济业务");
        }

        // 设置申请人和部门
        claim.setUserId(currentUserId);
        claim.setDeptId(currentUser.getDeptId());

        // 经理提交的报销单直接进入财务审批，跳过自己审批自己
        boolean success = expenseClaimService.submitClaim(claim, currentUser.getRole());
        return success ? Result.success(null) : Result.error("提交失败");
    }

    /**
     * 审批报销单（部门经理或财务操作）
     * 【重要】操作人ID从请求头获取，不再硬编码
     */
    @PostMapping("/approve")
    public Result approve(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("请先登录");
        }

        try {
            Long claimId = Long.valueOf(params.get("claimId").toString());
            boolean approved = Boolean.parseBoolean(params.get("approved").toString());
            String comment = params.getOrDefault("comment", "").toString();

            // 调用新的审批方法，operatorId 从请求头获取
            boolean success = expenseClaimService.approveClaim(claimId, approved, comment, currentUserId);

            return success ? Result.success(null) : Result.error("审批失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("接口异常: " + e.getMessage());
        }
    }

    /**
     * 旧审批接口兼容（已废弃，请使用 /approve）
     */
    @Deprecated
    @PostMapping("/audit-v2")
    public Result auditV2(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("请先登录");
        }

        try {
            Long id = Long.valueOf(params.get("id").toString());
            Integer status = Integer.valueOf(params.get("status").toString());
            String comment = params.getOrDefault("comment", "无备注").toString();

            boolean approved = status == ClaimStatus.FINANCE_APPROVED.getCode() ||
                              status == ClaimStatus.PENDING_FINANCE.getCode();
            boolean success = expenseClaimService.approveClaim(id, approved, comment, currentUserId);

            return success ? Result.success(null) : Result.error("审批失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("接口异常: " + e.getMessage());
        }
    }

    /**
     * 获取单个报销单详情
     */
    @GetMapping("/{id}")
    public Result<ExpenseClaim> getById(@PathVariable Integer id, HttpServletRequest request) {
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("请先登录");
        }

        ExpenseClaim claim = expenseClaimService.getById(id);
        if (claim == null) {
            return Result.error("报销单不存在");
        }
        return Result.success(claim);
    }

    /**
     * 获取高风险报销单（财务使用）
     */
    @GetMapping("/high-risk")
    public Result<List<ExpenseClaim>> getHighRisk(HttpServletRequest request) {
        Integer currentUserId = getCurrentUserId(request);
        if (currentUserId == null) {
            return Result.error("请先登录");
        }

        List<ExpenseClaim> list = expenseClaimService.getHighRiskClaims();
        return Result.success(list);
    }

    /**
     * 删除报销单（仅财务可操作，会留下审计痕迹）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteClaim(@PathVariable Integer id, HttpServletRequest request) {
        System.out.println("========== 删除报销单开始 ==========");
        System.out.println("[删除报销单] 接收到的ID: " + id);

        Integer currentUserId = getCurrentUserId(request);
        System.out.println("[删除报销单] 当前用户ID: " + currentUserId);

        if (currentUserId == null) {
            System.out.println("[删除报销单] 用户未登录");
            return Result.error("请先登录");
        }

        SysUser currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            System.out.println("[删除报销单] 用户不存在");
            return Result.error("用户不存在");
        }

        System.out.println("[删除报销单] 用户角色: " + currentUser.getRole());

        // 只有财务可以删除
        if (!currentUser.getRole().equals(UserRole.FINANCE.getCode())) {
            System.out.println("[删除报销单] 无权限");
            return Result.error("无权限，仅财务可删除报销单");
        }

        // 获取报销单信息用于记录日志
        ExpenseClaim claim = expenseClaimService.getById(id);
        if (claim == null) {
            System.out.println("[删除报销单] 报销单不存在");
            return Result.error("报销单不存在");
        }

        System.out.println("[删除报销单] 报销单信息: " + claim.getClaimNo() + ", 金额: " + claim.getAmount() + ", 状态: " + claim.getStatus());

        // 【修改】根据报销单状态处理预算回滚
        try {
            java.math.BigDecimal amount = new java.math.BigDecimal(claim.getAmount()).divide(new java.math.BigDecimal(100));
            Integer year = java.time.LocalDate.now().getYear();
            Integer status = claim.getStatus();

            // PENDING_MANAGER：冻结状态，释放冻结
            if (status == ClaimStatus.PENDING_MANAGER.getCode()) {
                System.out.println("[删除报销单] 状态=PENDING_MANAGER，释放冻结预算");
                departmentBudgetService.releaseFrozen(claim.getDeptId(), amount, year);
            }
            // PENDING_FINANCE：经理已审批通过，冻结=0，used已增加，回滚used
            else if (status == ClaimStatus.PENDING_FINANCE.getCode()) {
                System.out.println("[删除报销单] 状态=PENDING_FINANCE，回滚已使用预算");
                departmentBudgetService.rollbackUsed(claim.getDeptId(), amount, year);
            }
            // FINANCE_APPROVED：财务已审批，used已增加，回滚used
            else if (status == ClaimStatus.FINANCE_APPROVED.getCode()) {
                System.out.println("[删除报销单] 状态=FINANCE_APPROVED，回滚已使用预算");
                departmentBudgetService.rollbackUsed(claim.getDeptId(), amount, year);
            } else {
                System.out.println("[删除报销单] 其他状态，无需处理预算");
            }
        } catch (Exception e) {
            System.err.println("[删除报销单] 预算处理失败: " + e.getMessage());
        }

        // 先记录审计日志（在任何删除操作之前，确保日志一定会保存）
        AuditLog log = new AuditLog();
        try {
            log.setClaimId(id);
            log.setOperatorId(currentUserId);
            log.setActionType("DELETE");
            log.setAction(String.format("【财务删除】报销单 %s，金额=%.2f元，事由=%s",
                    claim.getClaimNo(),
                    claim.getAmount() / 100.0,
                    claim.getTitle()));
            log.setBeforeValue(objectMapper.writeValueAsString(claim));
            log.setAfterValue(null);
            System.out.println("[删除报销单] 准备插入审计日志...");
            int insertResult = auditLogMapper.insert(log);
            System.out.println("[删除报销单] 审计日志插入结果: " + insertResult + ", 日志ID: " + log.getId());
        } catch (Exception e) {
            System.err.println("[删除报销单] 记录审计日志失败: " + e.getMessage());
            e.printStackTrace();
        }

        // 删除关联的审批记录（外键约束）
        try {
            LambdaQueryWrapper<ApprovalRecord> approvalWrapper = new LambdaQueryWrapper<>();
            approvalWrapper.eq(ApprovalRecord::getClaimId, id);
            int approvalCount = approvalRecordMapper.delete(approvalWrapper);
            System.out.println("[删除报销单] 删除审批记录: " + approvalCount + "条");
        } catch (Exception e) {
            System.err.println("[删除报销单] 删除审批记录失败: " + e.getMessage());
        }

        // 删除关联的风险预警记录（外键约束）
        try {
            LambdaQueryWrapper<RiskAlert> alertWrapper = new LambdaQueryWrapper<>();
            alertWrapper.eq(RiskAlert::getClaimId, id);
            int alertCount = riskAlertMapper.delete(alertWrapper);
            System.out.println("[删除报销单] 删除预警记录: " + alertCount + "条");
        } catch (Exception e) {
            System.err.println("[删除报销单] 删除预警记录失败: " + e.getMessage());
        }

        // 执行删除
        boolean success = expenseClaimService.removeById(id);
        System.out.println("[删除报销单] 删除报销单结果: " + success);
        System.out.println("========== 删除报销单结束 ==========");

        if (success) {
            return Result.success(null);
        } else {
            return Result.error("删除失败");
        }
    }

    /**
     * 从请求属性获取当前用户ID
     * AuthInterceptor 会把用户ID存入 request.setAttribute("currentUserId", userId)
     */
    private Integer getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId != null) {
            return (Integer) userId;
        }
        return null;
    }
}