package com.finance.demo.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TrendQuery {
    private LocalDate startDate;
    private LocalDate endDate;
    private String granularity; // DAY, WEEK, MONTH, YEAR
    private Integer deptId;
}
