package com.finance.demo.controller;

import com.finance.demo.common.Result;
import com.finance.demo.entity.ExpensePolicyRule;
import com.finance.demo.entity.ExpenseCategory;
import com.finance.demo.mapper.ExpensePolicyRuleMapper;
import com.finance.demo.mapper.ExpenseCategoryMapper;
import com.finance.demo.service.rule.RuleContext;
import com.finance.demo.service.rule.RuleEngine;
import com.finance.demo.service.rule.RuleExecutionResult;
import com.finance.demo.service.rule.impl.RuleEngineImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rules")
@CrossOrigin
public class RuleController {

    @Autowired
    private ExpensePolicyRuleMapper ruleMapper;

    @Autowired
    private ExpenseCategoryMapper categoryMapper;

    @Autowired
    private RuleEngineImpl ruleEngine;

    @GetMapping
    public Result<List<ExpensePolicyRule>> getAllRules() {
        return Result.success(ruleMapper.selectList(null));
    }

    @GetMapping("/categories")
    public Result<List<ExpenseCategory>> getCategories() {
        return Result.success(categoryMapper.selectList(null));
    }

    @PostMapping("/validate")
    public Result<RuleExecutionResult> validateClaim(@RequestBody RuleContext context) {
        if (context.getCurrentDate() == null) {
            context.setCurrentDate(LocalDate.now());
        }
        RuleExecutionResult result = ruleEngine.execute(context);
        return Result.success(result);
    }

    @PutMapping("/reload")
    public Result<Void> reloadRules() {
        ruleEngine.reloadRules();
        return Result.success(null);
    }
}
