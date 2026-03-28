package com.finance.demo.service.risk;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RiskIndicator {
    private String indicatorCode;
    private String indicatorName;
    private BigDecimal value;
    private BigDecimal threshold;
    private boolean triggered;
    private BigDecimal contribution;

    public static RiskIndicator of(String code, String name, BigDecimal value, BigDecimal threshold, BigDecimal contribution) {
        RiskIndicator indicator = new RiskIndicator();
        indicator.setIndicatorCode(code);
        indicator.setIndicatorName(name);
        indicator.setValue(value);
        indicator.setThreshold(threshold);
        indicator.setTriggered(value.compareTo(threshold) >= 0);
        indicator.setContribution(contribution);
        return indicator;
    }
}
