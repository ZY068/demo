package com.finance.demo.enums;

import lombok.Getter;

/**
 * 报销单状态枚举
 * 两阶段审批流程：员工提交 -> 部门经理审批 -> 财务审计
 */
@Getter
public enum ClaimStatus {
    DRAFT(0, "草稿"),
    PENDING_MANAGER(1, "待部门经理审批"),
    PENDING_FINANCE(2, "待财务审计"),
    MANAGER_REJECTED(3, "部门经理驳回"),
    FINANCE_APPROVED(4, "已入账"),
    FINANCE_REJECTED(5, "财务驳回");

    private final int code;
    private final String desc;

    ClaimStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ClaimStatus fromCode(int code) {
        for (ClaimStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return DRAFT;
    }

    /**
     * 判断是否可以被部门经理审批
     */
    public boolean canManagerApprove() {
        return this == PENDING_MANAGER;
    }

    /**
     * 判断是否可以被财务审计
     */
    public boolean canFinanceAudit() {
        return this == PENDING_FINANCE;
    }

    /**
     * 判断是否是终态
     */
    public boolean isFinalStatus() {
        return this == FINANCE_APPROVED || this == FINANCE_REJECTED || this == MANAGER_REJECTED;
    }
}