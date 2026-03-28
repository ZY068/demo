package com.finance.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseTrendStats {
    private String granularity;
    private List<DataPoint> dataPoints = new ArrayList<>();
    private BigDecimal totalAmount;
    private Integer totalCount;
    private BigDecimal avgAmount;

    @Data
    public static class DataPoint {
        private String period;
        private BigDecimal amount;
        private Integer count;
        private BigDecimal trend; // 环比增长率
    }
}
