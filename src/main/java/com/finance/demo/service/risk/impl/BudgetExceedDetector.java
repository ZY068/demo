package com.finance.demo.service.risk.impl;

import com.finance.demo.entity.DepartmentBudget;
import com.finance.demo.entity.RiskRuleConfig;
import com.finance.demo.mapper.DepartmentBudgetMapper;
import com.finance.demo.service.risk.RiskIndicator;
import com.finance.demo.service.risk.RiskRule;
import com.finance.demo.service.rule.RuleContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class BudgetExceedDetector implements RiskRule {

    public static final String RISK_TYPE = "BUDGET_EXCEED";

    @Autowired
    private DepartmentBudgetMapper budgetMapper;

    @Override
    public String getRiskType() {
        return RISK_TYPE;
    }

    @Override
    public RiskIndicator detect(RuleContext context, RiskRuleConfig config) {
        if (context.getDepartment() == null) {
            return RiskIndicator.of(config.getRuleCode(), config.getRuleName(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        int currentYear = LocalDate.now().getYear();
        LambdaQueryWrapper<DepartmentBudget> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DepartmentBudget::getDeptId, context.getDepartment().getId())
               .eq(DepartmentBudget::getBudgetYear, currentYear);
        DepartmentBudget budget = budgetMapper.selectOne(wrapper);

        if (budget == null) {
            return RiskIndicator.of(config.getRuleCode(), config.getRuleName(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal usageRate = budget.getUsedAmount()
            .divide(budget.getBudgetAmount(), 4, BigDecimal.ROUND_HALF_UP);
        BigDecimal threshold = new BigDecimal("0.9"); // 90%阈值

        boolean triggered = usageRate.compareTo(threshold) >= 0;
        BigDecimal contribution = triggered ? config.getWeight() : BigDecimal.ZERO;

        return RiskIndicator.of(
            config.getRuleCode(),
            config.getRuleName(),
            usageRate.multiply(new BigDecimal("100")),
            threshold.multiply(new BigDecimal("100")),
            contribution
        );
    }
}
