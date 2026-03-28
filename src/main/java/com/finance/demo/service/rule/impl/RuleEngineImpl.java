package com.finance.demo.service.rule.impl;

import com.finance.demo.entity.ExpensePolicyRule;
import com.finance.demo.mapper.ExpensePolicyRuleMapper;
import com.finance.demo.service.rule.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Service
public class RuleEngineImpl implements RuleEngine {

    @Autowired
    private ExpensePolicyRuleMapper ruleMapper;

    @Autowired
    private Map<String, Rule> ruleHandlers;

    private volatile List<ExpensePolicyRule> cachedRules;

    @PostConstruct
    public void init() {
        reloadRules();
    }

    @Override
    public RuleExecutionResult execute(RuleContext context) {
        long startTime = System.currentTimeMillis();
        RuleExecutionResult result = RuleExecutionResult.success();

        List<ExpensePolicyRule> rules = getEnabledRules();
        for (ExpensePolicyRule rule : rules) {
            RuleCheckResult checkResult = checkRule(rule, context);
            if (!checkResult.isPassed()) {
                result.addViolation(new RuleViolation(
                    checkResult.getRuleCode(),
                    checkResult.getRuleName(),
                    checkResult.getMessage(),
                    rule.getPriority()
                ));
            }
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    public RuleCheckResult checkRule(ExpensePolicyRule rule, RuleContext context) {
        String ruleType = rule.getRuleType();
        Rule handler = ruleHandlers.get(ruleType);

        if (handler == null) {
            return RuleCheckResult.fail(rule.getRuleCode(), rule.getRuleName(),
                "未找到规则处理器: " + ruleType);
        }

        try {
            return handler.check(rule, context);
        } catch (Exception e) {
            return RuleCheckResult.fail(rule.getRuleCode(), rule.getRuleName(),
                "规则执行异常: " + e.getMessage());
        }
    }

    @Override
    public List<ExpensePolicyRule> getEnabledRules() {
        if (cachedRules == null) {
            reloadRules();
        }
        return cachedRules;
    }

    @Override
    public void reloadRules() {
        LambdaQueryWrapper<ExpensePolicyRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpensePolicyRule::getEnabled, true)
               .orderByAsc(ExpensePolicyRule::getPriority);
        this.cachedRules = ruleMapper.selectList(wrapper);
    }
}
