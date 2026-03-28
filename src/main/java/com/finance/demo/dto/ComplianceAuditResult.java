package com.finance.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ComplianceAuditResult {
    private Integer claimId;
    private boolean compliant;
    private List<Violation> violations = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();
    private LocalDateTime auditTime;

    @Data
    public static class Violation {
        private String ruleCode;
        private String ruleName;
        private String message;
        private String severity;
    }
}
