package com.finance.demo.service.rule;

import com.finance.demo.entity.ExpensePolicyRule;
import java.util.List;

public interface RuleEngine {
    /**
     * 执行所有启用的规则校验
     * @param context 规则上下文
     * @return 规则执行结果
     */
    RuleExecutionResult execute(RuleContext context);

    /**
     * 校验单条规则
     * @param rule 规则
     * @param context 上下文
     * @return 校验结果
     */
    RuleCheckResult checkRule(ExpensePolicyRule rule, RuleContext context);

    /**
     * 获取所有启用的规则
     * @return 规则列表
     */
    List<ExpensePolicyRule> getEnabledRules();

    /**
     * 重新加载规则缓存
     */
    void reloadRules();
}
