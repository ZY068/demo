package com.finance.demo.service.rule;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class RuleCheckResult {
    private boolean passed;
    private String ruleCode;
    private String ruleName;
    private String message;
    private List<RuleViolation> violations = new ArrayList<>();

    public static RuleCheckResult pass(String ruleCode, String ruleName) {
        RuleCheckResult result = new RuleCheckResult();
        result.setPassed(true);
        result.setRuleCode(ruleCode);
        result.setRuleName(ruleName);
        return result;
    }

    public static RuleCheckResult fail(String ruleCode, String ruleName, String message) {
        RuleCheckResult result = new RuleCheckResult();
        result.setPassed(false);
        result.setRuleCode(ruleCode);
        result.setRuleName(ruleName);
        result.setMessage(message);
        return result;
    }
}
