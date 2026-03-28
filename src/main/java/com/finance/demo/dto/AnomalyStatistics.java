package com.finance.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class AnomalyStatistics {
    private Integer totalAnomalies;
    private Map<String, Integer> anomalyTypeDistribution;
    private List<HighRiskUser> highRiskUsers = new ArrayList<>();
    private List<HighRiskDepartment> highRiskDepartments = new ArrayList<>();
    private BigDecimal anomalyRate;

    @Data
    public static class HighRiskUser {
        private Integer userId;
        private String userName;
        private Integer anomalyCount;
        private BigDecimal riskScore;
    }

    @Data
    public static class HighRiskDepartment {
        private Integer deptId;
        private String deptName;
        private Integer anomalyCount;
        private BigDecimal riskScore;
    }
}
