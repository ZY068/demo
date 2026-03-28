package com.finance.demo.service.risk.impl;

import com.finance.demo.entity.RiskRuleConfig;
import com.finance.demo.service.risk.RiskIndicator;
import com.finance.demo.service.risk.RiskRule;
import com.finance.demo.service.rule.RuleContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WeekendClaimDetector implements RiskRule {

    public static final String RISK_TYPE = "WEEKEND_CLAIM";

    @Override
    public String getRiskType() {
        return RISK_TYPE;
    }

    @Override
    public RiskIndicator detect(RuleContext context, RiskRuleConfig config) {
        int dayOfWeek = context.getDayOfWeek();

        // dayOfWeek: 1=周一, 7=周日, 0=无日期
        // 只有周六(6)和周日(7)才是周末
        boolean isWeekend = dayOfWeek == 6 || dayOfWeek == 7;

        System.out.println("[周末检测] dayOfWeek=" + dayOfWeek + ", isWeekend=" + isWeekend);

        // 周末预警固定贡献1分
        BigDecimal contribution = isWeekend ? BigDecimal.ONE : BigDecimal.ZERO;

        return RiskIndicator.of(
            config.getRuleCode(),
            config.getRuleName(),
            isWeekend ? BigDecimal.ONE : BigDecimal.ZERO,
            BigDecimal.ONE,  // 阈值固定为1
            contribution
        );
    }
}
