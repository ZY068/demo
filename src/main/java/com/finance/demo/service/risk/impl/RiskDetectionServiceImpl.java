package com.finance.demo.service.risk.impl;

import com.finance.demo.entity.*;
import com.finance.demo.mapper.*;
import com.finance.demo.service.risk.*;
import com.finance.demo.service.rule.RuleContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RiskDetectionServiceImpl implements RiskDetectionService {

    @Autowired
    private ExpenseClaimMapper claimMapper;

    @Autowired
    private RiskRuleConfigMapper ruleConfigMapper;

    @Autowired
    private RiskAlertMapper alertMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private ExpenseClaimDetailMapper detailMapper;

    @Autowired
    private List<RiskRule> riskRuleList;  // 注入所有 RiskRule

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, RiskRule> riskRules;  // 手动构建的 Map

    @PostConstruct
    public void init() {
        // 启动时手动构建 Map，key 是 getRiskType()
        riskRules = new HashMap<>();
        for (RiskRule rule : riskRuleList) {
            riskRules.put(rule.getRiskType(), rule);
        }
        System.out.println("[风险规则] 已注册检测器: " + riskRules.keySet());
    }

    @Override
    public RiskDetectionResult detectRisk(Integer claimId) {
        ExpenseClaim claim = claimMapper.selectById(claimId);
        if (claim == null) {
            System.err.println("[风险检测] 报销单不存在: " + claimId);
            return null;
        }

        System.out.println("[风险检测] 开始检测, claimId=" + claimId + ", amount=" + claim.getAmount());

        RiskDetectionResult result = new RiskDetectionResult();
        result.setClaimId(claimId);
        result.setRiskScore(BigDecimal.ZERO);

        RuleContext context = buildContext(claim);
        List<RiskRuleConfig> enabledRules = getEnabledRules();

        System.out.println("[风险检测] 启用的规则数量: " + enabledRules.size());
        System.out.println("[风险检测] 注册的检测器: " + riskRules.keySet());

        for (RiskRuleConfig config : enabledRules) {
            System.out.println("[风险检测] 检查规则: " + config.getRuleCode() + ", type=" + config.getRiskType());
            RiskRule rule = riskRules.get(config.getRiskType());
            if (rule != null) {
                RiskIndicator indicator = rule.detect(context, config);
                result.addIndicator(indicator);
                System.out.println("[风险检测] 规则" + config.getRuleCode() + "检测结果: triggered=" + indicator.isTriggered() + ", contribution=" + indicator.getContribution());

                if (indicator.isTriggered()) {
                    RiskAlert alert = createAlert(claim, indicator, config);
                    alertMapper.insert(alert);
                    result.getAlerts().add(alert);
                }
            } else {
                System.err.println("[风险检测] 未找到检测器: " + config.getRiskType());
            }
        }

        // 计算风险等级（根据预警的最高等级）
        result.setRiskLevel(calculateRiskLevel(result.getRiskScore(), result.getAlerts()));

        // 更新报销单风险评分
        claim.setRiskScore(result.getRiskScore());
        claimMapper.updateById(claim);

        System.out.println("[风险检测] 完成, 最终评分=" + result.getRiskScore());

        return result;
    }

    @Override
    @Async("riskDetectionExecutor")
    public void detectRiskAsync(Integer claimId) {
        try {
            detectRisk(claimId);
        } catch (Exception e) {
            // 异步执行失败不影响主流程，记录日志
            System.err.println("异步风险检测失败, claimId=" + claimId + ", error=" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public RiskDetectionResult reDetectRisk(Integer claimId) {
        // 删除旧的预警记录
        LambdaQueryWrapper<RiskAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskAlert::getClaimId, claimId);
        alertMapper.delete(wrapper);

        // 重新检测
        return detectRisk(claimId);
    }

    @Override
    public RiskDetectionResult getRiskDetail(Integer claimId) {
        ExpenseClaim claim = claimMapper.selectById(claimId);
        if (claim == null) {
            return null;
        }

        RiskDetectionResult result = new RiskDetectionResult();
        result.setClaimId(claimId);
        result.setRiskScore(claim.getRiskScore() != null ? claim.getRiskScore() : BigDecimal.ZERO);

        // 查询该报销单的所有预警
        LambdaQueryWrapper<RiskAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskAlert::getClaimId, claimId);
        List<RiskAlert> alerts = alertMapper.selectList(wrapper);
        result.setAlerts(alerts);

        // 根据预警的最高等级决定风险等级
        result.setRiskLevel(calculateRiskLevel(result.getRiskScore(), alerts));

        return result;
    }

    @Override
    public List<RiskDetectionResult> batchDetectRisk(List<Integer> claimIds) {
        List<RiskDetectionResult> results = new ArrayList<>();
        for (Integer claimId : claimIds) {
            RiskDetectionResult result = detectRisk(claimId);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    @Override
    public List<RiskAlert> getHighRiskAlerts() {
        // 返回所有未处理的风险预警（不再只返回高和严重）
        LambdaQueryWrapper<RiskAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskAlert::getHandlingStatus, 0)  // 待处理
               .orderByDesc(RiskAlert::getRiskLevel)  // 按风险等级倒序
               .orderByDesc(RiskAlert::getCreatedAt);
        return alertMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public boolean handleAlert(Integer alertId, Integer handlerId, String comment) {
        RiskAlert alert = alertMapper.selectById(alertId);
        if (alert == null) {
            return false;
        }

        alert.setHandlingStatus(2); // 已处理
        alert.setHandledBy(handlerId);
        alert.setHandledAt(LocalDate.now().atStartOfDay());
        alert.setHandledComment(comment);

        return alertMapper.updateById(alert) > 0;
    }

    private RuleContext buildContext(ExpenseClaim claim) {
        RuleContext context = new RuleContext();
        context.setClaim(claim);
        context.setCurrentDate(LocalDate.now());

        // 加载用户信息
        // context.setApplicant(userMapper.selectById(claim.getUserId()));

        // 加载部门信息
        if (claim.getDeptId() != null) {
            context.setDepartment(departmentMapper.selectById(claim.getDeptId()));
        }

        // 加载明细
        LambdaQueryWrapper<ExpenseClaimDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpenseClaimDetail::getClaimId, claim.getId());
        context.setDetails(detailMapper.selectList(wrapper));

        return context;
    }

    private List<RiskRuleConfig> getEnabledRules() {
        LambdaQueryWrapper<RiskRuleConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRuleConfig::getEnabled, 1);  // TINYINT 用 1 而不是 true
        return ruleConfigMapper.selectList(wrapper);
    }

    private RiskAlert createAlert(ExpenseClaim claim, RiskIndicator indicator, RiskRuleConfig config) {
        RiskAlert alert = new RiskAlert();
        alert.setClaimId(claim.getId());
        alert.setRiskType(config.getRiskType());
        alert.setRiskLevel(config.getRiskLevel());
        alert.setRiskScore(indicator.getContribution());
        alert.setAlertMessage(String.format("[%s] %s: 值=%.2f, 阈值=%.2f",
            config.getRuleName(), indicator.getIndicatorName(),
            indicator.getValue(), indicator.getThreshold()));
        alert.setHandlingStatus(0);
        try {
            alert.setTriggerRules(objectMapper.writeValueAsString(config));
        } catch (Exception e) {
            alert.setTriggerRules(config.getRuleCode());
        }
        return alert;
    }

    private String calculateRiskLevel(BigDecimal score, List<RiskAlert> alerts) {
        // 优先根据预警的最高等级决定风险等级
        if (alerts != null && !alerts.isEmpty()) {
            int maxLevel = alerts.stream()
                .mapToInt(a -> a.getRiskLevel() != null ? a.getRiskLevel() : 1)
                .max()
                .orElse(1);

            switch (maxLevel) {
                case 4: return "SEVERE";
                case 3: return "HIGH";
                case 2: return "MEDIUM";
                default: return "LOW";
            }
        }

        // 没有预警时，根据分数判断
        if (score == null || score.compareTo(BigDecimal.ZERO) == 0) {
            return "LOW";
        } else if (score.compareTo(new BigDecimal("2")) < 0) {
            return "LOW";
        } else if (score.compareTo(new BigDecimal("4")) < 0) {
            return "MEDIUM";
        } else if (score.compareTo(new BigDecimal("6")) < 0) {
            return "HIGH";
        } else {
            return "SEVERE";
        }
    }
}
