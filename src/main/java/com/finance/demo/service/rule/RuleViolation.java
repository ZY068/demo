package com.finance.demo.service.rule;

import lombok.Data;

@Data
public class RuleViolation {
    private String ruleCode;
    private String ruleName;
    private String errorMessage;
    private Integer priority;

    public RuleViolation() {}

    public RuleViolation(String ruleCode, String ruleName, String errorMessage, Integer priority) {
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.errorMessage = errorMessage;
        this.priority = priority;
    }
}
