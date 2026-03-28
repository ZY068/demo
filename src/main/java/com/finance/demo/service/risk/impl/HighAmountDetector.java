package com.finance.demo.service.risk.impl;

import com.finance.demo.entity.RiskRuleConfig;
import com.finance.demo.service.risk.RiskIndicator;
import com.finance.demo.service.risk.RiskRule;
import com.finance.demo.service.rule.RuleContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HighAmountDetector implements RiskRule {

    public static final String RISK_TYPE = "HIGH_AMOUNT";

    @Override
    public String getRiskType() {
        return RISK_TYPE;
    }

    @Override
    public RiskIndicator detect(RuleContext context, RiskRuleConfig config) {
        BigDecimal amount = context.getTotalAmount();
        BigDecimal threshold = new BigDecimal("50000"); // 5万以上视为高金额

        boolean triggered = amount.compareTo(threshold) >= 0;
        BigDecimal contribution = triggered ? config.getWeight() : BigDecimal.ZERO;

        return RiskIndicator.of(
            config.getRuleCode(),
            config.getRuleName(),
            amount,
            threshold,
            contribution
        );
    }
}
