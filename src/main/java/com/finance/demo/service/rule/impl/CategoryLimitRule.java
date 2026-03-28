package com.finance.demo.service.rule.impl;

import com.finance.demo.entity.ExpensePolicyRule;
import com.finance.demo.entity.ExpenseClaimDetail;
import com.finance.demo.entity.ExpenseCategory;
import com.finance.demo.mapper.ExpenseCategoryMapper;
import com.finance.demo.mapper.PolicyRuleConditionMapper;
import com.finance.demo.service.rule.Rule;
import com.finance.demo.service.rule.RuleCheckResult;
import com.finance.demo.service.rule.RuleContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CategoryLimitRule implements Rule {

    public static final String RULE_TYPE = "CATEGORY_LIMIT";

    @Autowired
    private PolicyRuleConditionMapper conditionMapper;

    @Autowired
    private ExpenseCategoryMapper categoryMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getRuleType() {
        return RULE_TYPE;
    }

    @Override
    public RuleCheckResult check(ExpensePolicyRule rule, RuleContext context) {
        if (context.getDetails() == null || context.getDetails().isEmpty()) {
            return RuleCheckResult.pass(rule.getRuleCode(), rule.getRuleName());
        }

        Map<Integer, BigDecimal> categoryAmounts = context.getDetails().stream()
            .collect(Collectors.groupingBy(
                ExpenseClaimDetail::getCategoryId,
                Collectors.reducing(BigDecimal.ZERO, ExpenseClaimDetail::getAmount, BigDecimal::add)
            ));

        for (Map.Entry<Integer, BigDecimal> entry : categoryAmounts.entrySet()) {
            ExpenseCategory category = categoryMapper.selectById(entry.getKey());
            if (category != null && category.getMaxAmountPerClaim() != null) {
                if (entry.getValue().compareTo(category.getMaxAmountPerClaim()) > 0) {
                    String message = String.format("费用类别[%s]金额%.2f超过单次上限%.2f",
                        category.getName(), entry.getValue(), category.getMaxAmountPerClaim());
                    String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage() : message;
                    return RuleCheckResult.fail(rule.getRuleCode(), rule.getRuleName(), errorMsg);
                }
            }
        }

        return RuleCheckResult.pass(rule.getRuleCode(), rule.getRuleName());
    }
}
