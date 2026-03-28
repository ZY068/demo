package com.finance.demo.service.rule;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class RuleExecutionResult {
    private boolean passed;
    private List<RuleViolation> violations = new ArrayList<>();
    private Long executionTimeMs;

    public void addViolation(RuleViolation violation) {
        this.violations.add(violation);
        this.passed = false;
    }

    public void addViolations(List<RuleViolation> violations) {
        if (violations != null && !violations.isEmpty()) {
            this.violations.addAll(violations);
            this.passed = false;
        }
    }

    public static RuleExecutionResult success() {
        RuleExecutionResult result = new RuleExecutionResult();
        result.setPassed(true);
        return result;
    }
}
