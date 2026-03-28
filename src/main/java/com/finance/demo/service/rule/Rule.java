package com.finance.demo.service.rule;

import com.finance.demo.entity.ExpensePolicyRule;

public interface Rule {
    /**
     * 获取规则类型
     */
    String getRuleType();

    /**
     * 执行规则校验
     * @param rule 配置的规则
     * @param context 规则上下文
     * @return 校验结果
     */
    RuleCheckResult check(ExpensePolicyRule rule, RuleContext context);
}
