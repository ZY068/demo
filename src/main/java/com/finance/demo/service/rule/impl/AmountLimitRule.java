package com.finance.demo.service.rule.impl;

import com.finance.demo.entity.ExpensePolicyRule;
import com.finance.demo.entity.PolicyRuleCondition;
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

@Component
public class AmountLimitRule implements Rule {

    public static final String RULE_TYPE = "AMOUNT_LIMIT";

    @Autowired
    private PolicyRuleConditionMapper conditionMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getRuleType() {
        return RULE_TYPE;
    }

    @Override
    public RuleCheckResult check(ExpensePolicyRule rule, RuleContext context) {
        BigDecimal amount = context.getTotalAmount();

        LambdaQueryWrapper<PolicyRuleCondition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PolicyRuleCondition::getRuleId, rule.getId());
        List<PolicyRuleCondition> conditions = conditionMapper.selectList(wrapper);

        for (PolicyRuleCondition condition : conditions) {
            if ("AMOUNT_RANGE".equals(condition.getConditionType())) {
                RuleCheckResult result = checkAmountRange(condition, amount, rule);
                if (!result.isPassed()) {
                    return result;
                }
            }
        }

        return RuleCheckResult.pass(rule.getRuleCode(), rule.getRuleName());
    }

    private RuleCheckResult checkAmountRange(PolicyRuleCondition condition, BigDecimal amount, ExpensePolicyRule rule) {
        try {
            String fieldValue = condition.getFieldValue();
            List<BigDecimal> ranges = objectMapper.readValue(fieldValue, new TypeReference<List<BigDecimal>>() {});

            if (ranges.size() >= 2) {
                BigDecimal min = ranges.get(0);
                BigDecimal max = ranges.get(1);

                if (amount.compareTo(min) < 0 || amount.compareTo(max) > 0) {
                    String message = String.format("金额%.2f超出允许范围[%.2f, %.2f]",
                        amount, min, max);
                    String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage() : message;
                    return RuleCheckResult.fail("AMOUNT_LIMIT", "金额限制规则", errorMsg);
                }
            }
        } catch (Exception e) {
            return RuleCheckResult.fail("AMOUNT_LIMIT", "金额限制规则", "规则解析失败: " + e.getMessage());
        }
        return RuleCheckResult.pass("AMOUNT_LIMIT", "金额限制规则");
    }
}
