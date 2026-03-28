package com.finance.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.finance.demo.entity.DepartmentBudget;
import com.finance.demo.mapper.DepartmentBudgetMapper;
import com.finance.demo.service.DepartmentBudgetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class DepartmentBudgetServiceImpl extends ServiceImpl<DepartmentBudgetMapper, DepartmentBudget> implements DepartmentBudgetService {

    @Override
    @Transactional
    public boolean freezeBudget(Integer deptId, BigDecimal amount, Integer year) {
        LambdaUpdateWrapper<DepartmentBudget> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DepartmentBudget::getDeptId, deptId)
               .eq(DepartmentBudget::getBudgetYear, year)
               .setSql("frozen_amount = frozen_amount + " + amount);
        return this.update(wrapper);
    }

    @Override
    @Transactional
    public boolean deductBudget(Integer deptId, BigDecimal amount, Integer year) {
        LambdaUpdateWrapper<DepartmentBudget> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DepartmentBudget::getDeptId, deptId)
               .eq(DepartmentBudget::getBudgetYear, year)
               .setSql("used_amount = used_amount + " + amount)
               .setSql("frozen_amount = frozen_amount - " + amount);
        return this.update(wrapper);
    }

    @Override
    @Transactional
    public boolean releaseFrozen(Integer deptId, BigDecimal amount, Integer year) {
        LambdaUpdateWrapper<DepartmentBudget> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DepartmentBudget::getDeptId, deptId)
               .eq(DepartmentBudget::getBudgetYear, year)
               .setSql("frozen_amount = frozen_amount - " + amount);
        return this.update(wrapper);
    }

    @Override
    @Transactional
    public boolean rollbackUsed(Integer deptId, BigDecimal amount, Integer year) {
        LambdaUpdateWrapper<DepartmentBudget> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DepartmentBudget::getDeptId, deptId)
               .eq(DepartmentBudget::getBudgetYear, year)
               .setSql("used_amount = used_amount - " + amount);
        return this.update(wrapper);
    }

    @Override
    public BigDecimal getAvailableBudget(Integer deptId, Integer year) {
        DepartmentBudget budget = getDepartmentBudget(deptId, year);
        if (budget == null || budget.getBudgetAmount() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal available = budget.getBudgetAmount()
                .subtract(budget.getUsedAmount() != null ? budget.getUsedAmount() : BigDecimal.ZERO)
                .subtract(budget.getFrozenAmount() != null ? budget.getFrozenAmount() : BigDecimal.ZERO);
        return available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available;
    }

    @Override
    public DepartmentBudget getDepartmentBudget(Integer deptId, Integer year) {
        LambdaQueryWrapper<DepartmentBudget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DepartmentBudget::getDeptId, deptId)
               .eq(DepartmentBudget::getBudgetYear, year);
        return this.getOne(wrapper);
    }
}
