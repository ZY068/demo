package com.finance.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.finance.demo.entity.DepartmentBudget;
import java.math.BigDecimal;

public interface DepartmentBudgetService extends IService<DepartmentBudget> {
    /**
     * 冻结预算（提交申请时）
     * @param deptId 部门ID
     * @param amount 金额
     * @param year 年度
     * @return 是否成功
     */
    boolean freezeBudget(Integer deptId, BigDecimal amount, Integer year);

    /**
     * 确认扣减预算（审批通过时）
     * @param deptId 部门ID
     * @param amount 金额
     * @param year 年度
     * @return 是否成功
     */
    boolean deductBudget(Integer deptId, BigDecimal amount, Integer year);

    /**
     * 释放冻结预算（驳回/取消时）
     * @param deptId 部门ID
     * @param amount 金额
     * @param year 年度
     * @return 是否成功
     */
    boolean releaseFrozen(Integer deptId, BigDecimal amount, Integer year);

    /**
     * 回滚已使用预算（删除报销单时）
     * @param deptId 部门ID
     * @param amount 金额
     * @param year 年度
     * @return 是否成功
     */
    boolean rollbackUsed(Integer deptId, BigDecimal amount, Integer year);

    /**
     * 获取可用预算
     * @param deptId 部门ID
     * @param year 年度
     * @return 可用预算金额
     */
    BigDecimal getAvailableBudget(Integer deptId, Integer year);

    /**
     * 获取部门预算
     * @param deptId 部门ID
     * @param year 年度
     * @return 预算对象
     */
    DepartmentBudget getDepartmentBudget(Integer deptId, Integer year);
}
