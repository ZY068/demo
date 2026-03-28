package com.finance.demo.service.rule.impl;

import com.finance.demo.entity.ExpensePolicyRule;
import com.finance.demo.service.rule.Rule;
import com.finance.demo.service.rule.RuleCheckResult;
import com.finance.demo.service.rule.RuleContext;
import org.springframework.stereotype.Component;

@Component
public class TimeLimitRule implements Rule {

    public static final String RULE_TYPE = "TIME_LIMIT";

    @Override
    public String getRuleType() {
        return RULE_TYPE;
    }

    @Override
    public RuleCheckResult check(ExpensePolicyRule rule, RuleContext context) {
        int dayOfWeek = context.getDayOfWeek();

        // 周末不能报销 (周六=6, 周日=7)
        if (dayOfWeek == 6 || dayOfWeek == 7) {
            String message = "周末提交的报销单需要额外审批";
            String errorMsg = rule.getErrorMessage() != null ? rule.getErrorMessage() : message;
            return RuleCheckResult.fail(rule.getRuleCode(), rule.getRuleName(), errorMsg);
        }

        return RuleCheckResult.pass(rule.getRuleCode(), rule.getRuleName());
    }
}
