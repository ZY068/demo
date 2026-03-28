package com.finance.demo.service.risk;

import com.finance.demo.entity.RiskRuleConfig;
import com.finance.demo.service.rule.RuleContext;

public interface RiskRule {
    /**
     * 获取风险类型
     */
    String getRiskType();

    /**
     * 检测风险
     * @param context 规则上下文
     * @param config 风险规则配置
     * @return 风险指标
     */
    RiskIndicator detect(RuleContext context, RiskRuleConfig config);
}
