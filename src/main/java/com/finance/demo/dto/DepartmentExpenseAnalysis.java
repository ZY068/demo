package com.finance.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class DepartmentExpenseAnalysis {
    private Integer deptId;
    private String deptName;
    private Integer year;
    private BigDecimal budgetAmount;
    private BigDecimal usedAmount;
    private BigDecimal frozenAmount;
    private BigDecimal availableAmount;
    private BigDecimal usageRate;
    private List<CategoryBreakdown> categoryBreakdowns = new ArrayList<>();
    private BigDecimal yearOverYearGrowth;
    private BigDecimal monthOverMonthGrowth;

    @Data
    public static class CategoryBreakdown {
        private String categoryName;
        private BigDecimal amount;
        private BigDecimal percentage;
        private Integer claimCount;
    }
}
