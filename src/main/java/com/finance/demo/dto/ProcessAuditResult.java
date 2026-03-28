package com.finance.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessAuditResult {
    private Integer claimId;
    private boolean processComplete;
    private List<ApprovalStep> approvalChain = new ArrayList<>();
    private Integer approvalStepCount;
    private Integer actualApprovers;
    private Integer requiredApprovers;
    private List<String> processGaps = new ArrayList<>();
    private LocalDateTime auditTime;

    @Data
    public static class ApprovalStep {
        private Integer level;
        private String approverName;
        private String action;
        private String comment;
        private LocalDateTime timestamp;
    }
}
