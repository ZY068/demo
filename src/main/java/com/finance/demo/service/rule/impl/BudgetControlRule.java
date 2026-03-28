package com.finance.demo.service.rule.impl;

import com.finance.demo.entity.ExpensePolicyRule;
import com.finance.demo.entity.DepartmentBudget;
import com.finance.demo.mapper.DepartmentBudgetMapper;
import com.finance.demo.service.rule.Rule;
import com.finance.demo.service.rule.RuleCheckResult;
import com.finance.demo.service.rule.RuleContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class BudgetControlRule implements Rule {

    public static final String RULE_TYPE = "BUDGET_CONTROL";

    @Autowired
    private DepartmentBudgetMapper budgetMapper;

    @Override
    public String getRuleType() {
        return RULE_TYPE;
    }

    @Override
    public RuleCheckResult check(ExpensePolicyRule rule, RuleContext context) {
        if (context.getDepartment() == null) {
            return RuleCheckResult.pass(rule.getRuleCode(), rule.getRuleName());
        }

        int currentYear = LocalDate.now().getYear();

        LambdaQueryWrapper<DepartmentBudget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DepartmentBudget::getDeptId, context.getDepartment().getId())
               .eq(DepartmentBudget::getBudgetYear, currentYear);
        DepartmentBudget budget = budgetMapper.selectOne(wrapper);

        if (budget == null) {
            return RuleCheckResult.pass(rule.getRuleCode(), rule.getRuleName());
        }

        BigDecimal available = budget.getBudgetAmount()
            .subtract(budget.getUsedAmount())
            .subtract(budget.getFrozenAmount());

        BigDecimal claimAmount = context.getTotalAmount();

        if (claimAmount.compareTo(available) > 0) {
            String message = String.format("部门预算不足，申请金额%.2f，可用金额%.2f",
                claimAmount, available);
            String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage() : message;
            return RuleCheckResult.fail(rule.getRuleCode(), rule.getRuleName(), errorMsg);
        }

        if (budget.getUsedAmount().add(budget.getFrozenAmount()).compareTo(
                budget.getBudgetAmount().multiply(new BigDecimal("0.9"))) >= 0) {
            String message = String.format("部门预算使用率超过90%%，当前已用%.2f/%.2f",
                budget.getUsedAmount().add(budget.getFrozenAmount()), budget.getBudgetAmount());
            String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage() : message;
            return RuleCheckResult.fail(rule.getRuleCode(), rule.getRuleName(), errorMsg);
        }

        return RuleCheckResult.pass(rule.getRuleCode(), rule.getRuleName());
    }
}
