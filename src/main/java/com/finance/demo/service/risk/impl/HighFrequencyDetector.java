package com.finance.demo.service.risk.impl;

import com.finance.demo.entity.RiskRuleConfig;
import com.finance.demo.mapper.ExpenseClaimMapper;
import com.finance.demo.service.risk.RiskIndicator;
import com.finance.demo.service.risk.RiskRule;
import com.finance.demo.service.rule.RuleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HighFrequencyDetector implements RiskRule {

    public static final String RISK_TYPE = "HIGH_FREQUENCY";

    @Autowired
    private ExpenseClaimMapper claimMapper;

    @Override
    public String getRiskType() {
        return RISK_TYPE;
    }

    @Override
    public RiskIndicator detect(RuleContext context, RiskRuleConfig config) {
        // 检测同一用户一周内报销次数是否超过5次
        int claimCountInWeek = claimMapper.selectCountInWeek(context.getClaim().getUserId());

        boolean triggered = claimCountInWeek > 5;
        BigDecimal contribution = triggered ? config.getWeight() : BigDecimal.ZERO;

        return RiskIndicator.of(
            config.getRuleCode(),
            config.getRuleName(),
            new BigDecimal(claimCountInWeek),
            new BigDecimal("5"),
            contribution
        );
    }
}
