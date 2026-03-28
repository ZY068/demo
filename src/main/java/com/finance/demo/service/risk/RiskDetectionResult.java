package com.finance.demo.service.risk;

import com.finance.demo.entity.RiskAlert;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class RiskDetectionResult {
    private Integer claimId;
    private BigDecimal riskScore;
    private String riskLevel;
    private List<RiskIndicator> indicators = new ArrayList<>();
    private List<RiskAlert> alerts = new ArrayList<>();

    public void addIndicator(RiskIndicator indicator) {
        this.indicators.add(indicator);
        if (indicator.isTriggered()) {
            this.riskScore = this.riskScore != null
                ? this.riskScore.add(indicator.getContribution())
                : indicator.getContribution();
        }
    }
}
