package com.finance.demo.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 * 角色权限分离：
 * - 普通员工：提交报销单、查看自己的报销单
 * - 部门经理：审批本部门员工的报销单（不能审批自己的）
 * - 财务内审：最终审批、风险分析、假账识别
 * - IT管理员：后台管理、规则配置，不能经手经济业务
 */
@Getter
public enum UserRole {
    EMPLOYEE(0, "普通员工"),
    MANAGER(1, "部门经理"),
    FINANCE(2, "财务内审"),
    ADMIN(9, "IT管理员");

    private final int code;
    private final String desc;

    UserRole(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserRole fromCode(int code) {
        for (UserRole role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        return EMPLOYEE;
    }

    /**
     * 判断是否可以审批报销单
     */
    public boolean canApproveExpense() {
        return this == MANAGER || this == FINANCE;
    }

    /**
     * 判断是否可以参与经济业务（提交报销单）
     */
    public boolean canHandleFinancialBusiness() {
        return this != ADMIN;
    }

    /**
     * 判断是否是IT管理员
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * 判断是否是财务
     */
    public boolean isFinance() {
        return this == FINANCE;
    }

    /**
     * 判断是否是部门经理
     */
    public boolean isManager() {
        return this == MANAGER;
    }
}
