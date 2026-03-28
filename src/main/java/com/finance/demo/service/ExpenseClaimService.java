package com.finance.demo.service;

import com.finance.demo.entity.ExpenseClaim;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface ExpenseClaimService extends IService<ExpenseClaim> {

    /**
     * 提交报销单
     * @param claim 报销单实体
     * @param userRole 提交人角色（经理提交跳过自己审批）
     */
    boolean submitClaim(ExpenseClaim claim, Integer userRole);

    /**
     * 审批报销单（部门经理或财务操作）
     * @param claimId 报销单ID
     * @param approved 是否通过
     * @param comment 审批意见
     * @param operatorId 操作人ID（从JWT获取，不能硬编码）
     * @return 是否成功
     */
    boolean approveClaim(Long claimId, boolean approved, String comment, Integer operatorId);

    /**
     * 旧方法兼容（已废弃，请使用 approveClaim）
     */
    @Deprecated
    boolean auditClaim(Long claimId, Integer status, String comment, Integer operatorId);

    /**
     * 获取高风险报销单（周末报销异常）
     */
    List<ExpenseClaim> getHighRiskClaims();

    /**
     * 获取待审批的报销单列表（按角色过滤）
     * @param operatorId 操作人ID
     * @return 待审批列表
     */
    List<ExpenseClaim> getPendingClaims(Integer operatorId);

    /**
     * 获取用户自己的报销单
     * @param userId 用户ID
     * @return 报销单列表
     */
    List<ExpenseClaim> getMyClaims(Integer userId);
}