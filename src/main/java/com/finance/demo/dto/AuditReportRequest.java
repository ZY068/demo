package com.finance.demo.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AuditReportRequest {
    private String reportType; // COMPLIANCE, PROCESS, DATA_ANALYSIS
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer deptId;
    private Integer generatedBy;
}
