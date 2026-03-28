package com.finance.demo.service.audit.impl;

import com.finance.demo.dto.*;
import com.finance.demo.entity.*;
import com.finance.demo.mapper.*;
import com.finance.demo.service.audit.AuditAnalysisService;
import com.finance.demo.service.rule.RuleContext;
import com.finance.demo.service.rule.RuleEngine;
import com.finance.demo.service.rule.RuleExecutionResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuditAnalysisServiceImpl implements AuditAnalysisService {

    @Autowired
    private ExpenseClaimMapper claimMapper;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private RiskAlertMapper riskAlertMapper;

    @Autowired
    private DepartmentBudgetMapper budgetMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private ExpenseClaimDetailMapper detailMapper;

    @Autowired
    private ExpenseCategoryMapper categoryMapper;

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public ComplianceAuditResult auditCompliance(Integer claimId) {
        ExpenseClaim claim = claimMapper.selectById(claimId);
        if (claim == null) {
            return null;
        }

        ComplianceAuditResult result = new ComplianceAuditResult();
        result.setClaimId(claimId);
        result.setAuditTime(LocalDateTime.now());
        result.setCompliant(true);

        // 构建规则上下文
        RuleContext context = buildContext(claim);

        // 执行规则引擎校验
        RuleExecutionResult ruleResult = ruleEngine.execute(context);

        if (!ruleResult.isPassed()) {
            result.setCompliant(false);
            for (var violation : ruleResult.getViolations()) {
                ComplianceAuditResult.Violation v = new ComplianceAuditResult.Violation();
                v.setRuleCode(violation.getRuleCode());
                v.setRuleName(violation.getRuleName());
                v.setMessage(violation.getErrorMessage());
                v.setSeverity(violation.getPriority() != null && violation.getPriority() < 50 ? "HIGH" : "MEDIUM");
                result.getViolations().add(v);
            }
        }

        // 检查发票完整性
        checkInvoiceIntegrity(claim, result);

        // 添加建议
        if (!result.isCompliant()) {
            result.getRecommendations().add("请根据违规信息修改报销单后重新提交");
        }

        return result;
    }

    @Override
    public ProcessAuditResult auditProcess(Integer claimId) {
        ExpenseClaim claim = claimMapper.selectById(claimId);
        if (claim == null) {
            return null;
        }

        ProcessAuditResult result = new ProcessAuditResult();
        result.setClaimId(claimId);
        result.setAuditTime(LocalDateTime.now());

        // 获取审批记录
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getClaimId, claimId)
               .orderByAsc(AuditLog::getCreatedAt);
        List<AuditLog> auditLogs = auditLogMapper.selectList(wrapper);

        // 构建审批链
        int level = 1;
        for (AuditLog log : auditLogs) {
            ProcessAuditResult.ApprovalStep step = new ProcessAuditResult.ApprovalStep();
            step.setLevel(level++);
            step.setAction(log.getAction());
            step.setComment(log.getAction());
            step.setTimestamp(log.getCreatedAt());
            result.getApprovalChain().add(step);
        }

        result.setActualApprovers(auditLogs.size());

        // 简单流程完整性检查
        // 实际实现应根据审批流程配置来判断
        if (claim.getStatus() == 1) { // 已通过
            result.setRequiredApprovers(2); // 默认需要2级审批
            result.setApprovalStepCount(2);
            result.setProcessComplete(auditLogs.size() >= 2);
        } else if (claim.getStatus() == 2) { // 已驳回
            result.setProcessComplete(true);
            result.setRequiredApprovers(1);
            result.setApprovalStepCount(1);
        } else {
            result.setProcessComplete(false);
            result.setRequiredApprovers(2);
            result.setApprovalStepCount(0);
        }

        // 检查流程缺口
        if (!result.isProcessComplete() && result.getActualApprovers() < result.getRequiredApprovers()) {
            result.getProcessGaps().add("审批流程未完成，缺少 " +
                (result.getRequiredApprovers() - result.getActualApprovers()) + " 级审批");
        }

        return result;
    }

    @Override
    public String generateReport(AuditReportRequest request) {
        String reportCode = "RPT" + System.currentTimeMillis();

        AuditReport report = new AuditReport();
        report.setReportCode(reportCode);
        report.setReportType(request.getReportType());
        report.setReportPeriodStart(request.getStartDate());
        report.setReportPeriodEnd(request.getEndDate());
        report.setGeneratedBy(request.getGeneratedBy());
        report.setStatus(false);

        try {
            StringBuilder summary = new StringBuilder();
            summary.append("审计报告\n");
            summary.append("报告类型: ").append(request.getReportType()).append("\n");
            summary.append("周期: ").append(request.getStartDate()).append(" 至 ").append(request.getEndDate()).append("\n\n");

            // 生成统计数据
            TrendQuery query = new TrendQuery();
            query.setStartDate(request.getStartDate());
            query.setEndDate(request.getEndDate());
            query.setDeptId(request.getDeptId());
            query.setGranularity("DAY");

            ExpenseTrendStats trendStats = getExpenseTrend(query);
            summary.append("报销总金额: ").append(trendStats.getTotalAmount()).append(" 元\n");
            summary.append("报销单数量: ").append(trendStats.getTotalCount()).append(" 张\n");
            summary.append("平均单额: ").append(trendStats.getAvgAmount()).append(" 元\n");

            AnomalyStatistics anomalyStats = getAnomalyStats(query);
            summary.append("异常单据: ").append(anomalyStats.getTotalAnomalies()).append(" 张\n");
            summary.append("异常率: ").append(anomalyStats.getAnomalyRate()).append("%\n");

            report.setSummary(summary.toString());
            report.setFindings("[]"); // JSON格式
            report.setRecommendations("[]"); // JSON格式

        } catch (Exception e) {
            report.setSummary("报告生成失败: " + e.getMessage());
        }

        // 保存报告
        // reportMapper.insert(report);

        return reportCode;
    }

    @Override
    public ExpenseTrendStats getExpenseTrend(TrendQuery query) {
        ExpenseTrendStats stats = new ExpenseTrendStats();
        stats.setGranularity(query.getGranularity() != null ? query.getGranularity() : "DAY");

        LambdaQueryWrapper<ExpenseClaim> wrapper = new LambdaQueryWrapper<>();
        if (query.getStartDate() != null) {
            wrapper.ge(ExpenseClaim::getExpenseDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(ExpenseClaim::getExpenseDate, query.getEndDate());
        }
        if (query.getDeptId() != null) {
            wrapper.eq(ExpenseClaim::getDeptId, query.getDeptId());
        }

        List<ExpenseClaim> claims = claimMapper.selectList(wrapper);

        BigDecimal total = BigDecimal.ZERO;
        Map<String, List<ExpenseClaim>> grouped = new LinkedHashMap<>();

        for (ExpenseClaim claim : claims) {
            String period = formatPeriod(claim.getExpenseDate(), stats.getGranularity());
            grouped.computeIfAbsent(period, k -> new ArrayList<>()).add(claim);

            if (claim.getAmount() != null) {
                total = total.add(new BigDecimal(claim.getAmount()));
            }
        }

        String prevPeriod = null;
        BigDecimal prevAmount = null;

        for (Map.Entry<String, List<ExpenseClaim>> entry : grouped.entrySet()) {
            ExpenseTrendStats.DataPoint point = new ExpenseTrendStats.DataPoint();
            point.setPeriod(entry.getKey());

            BigDecimal periodTotal = entry.getValue().stream()
                .map(c -> new BigDecimal(c.getAmount() != null ? c.getAmount() : 0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            point.setAmount(periodTotal);
            point.setCount(entry.getValue().size());

            if (prevAmount != null && prevAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal trend = periodTotal.subtract(prevAmount)
                    .divide(prevAmount, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                point.setTrend(trend);
            } else {
                point.setTrend(BigDecimal.ZERO);
            }

            stats.getDataPoints().add(point);
            prevPeriod = entry.getKey();
            prevAmount = periodTotal;
        }

        stats.setTotalAmount(total.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        stats.setTotalCount(claims.size());
        stats.setAvgAmount(claims.isEmpty() ? BigDecimal.ZERO :
            total.divide(new BigDecimal(claims.size() * 100), 2, RoundingMode.HALF_UP));

        return stats;
    }

    @Override
    public DepartmentExpenseAnalysis getDepartmentAnalysis(Integer deptId, Integer year) {
        Department dept = departmentMapper.selectById(deptId);
        DepartmentExpenseAnalysis analysis = new DepartmentExpenseAnalysis();
        analysis.setDeptId(deptId);
        analysis.setDeptName(dept != null ? dept.getDeptName() : "");
        analysis.setYear(year != null ? year : LocalDate.now().getYear());

        // 获取预算信息
        LambdaQueryWrapper<DepartmentBudget> budgetWrapper = new LambdaQueryWrapper<>();
        budgetWrapper.eq(DepartmentBudget::getDeptId, deptId)
                     .eq(DepartmentBudget::getBudgetYear, analysis.getYear());
        DepartmentBudget budget = budgetMapper.selectOne(budgetWrapper);

        if (budget != null) {
            analysis.setBudgetAmount(budget.getBudgetAmount());
            analysis.setUsedAmount(budget.getUsedAmount());
            analysis.setFrozenAmount(budget.getFrozenAmount());
            analysis.setAvailableAmount(budget.getBudgetAmount()
                .subtract(budget.getUsedAmount())
                .subtract(budget.getFrozenAmount()));
            if (budget.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0) {
                analysis.setUsageRate(budget.getUsedAmount()
                    .divide(budget.getBudgetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")));
            }
        }

        // 获取费用类别分布
        LambdaQueryWrapper<ExpenseClaimDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(ExpenseClaimDetail::getCategoryId, deptId); // 简化，实际需要关联查询
        List<ExpenseClaimDetail> details = detailMapper.selectList(detailWrapper);

        Map<Integer, BigDecimal> categoryAmounts = details.stream()
            .collect(Collectors.groupingBy(
                ExpenseClaimDetail::getCategoryId,
                Collectors.reducing(BigDecimal.ZERO, ExpenseClaimDetail::getAmount, BigDecimal::add)
            ));

        BigDecimal totalCategoryAmount = categoryAmounts.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Map.Entry<Integer, BigDecimal> entry : categoryAmounts.entrySet()) {
            ExpenseCategory category = categoryMapper.selectById(entry.getKey());
            if (category != null) {
                DepartmentExpenseAnalysis.CategoryBreakdown breakdown =
                    new DepartmentExpenseAnalysis.CategoryBreakdown();
                breakdown.setCategoryName(category.getName());
                breakdown.setAmount(entry.getValue());
                breakdown.setPercentage(totalCategoryAmount.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().divide(totalCategoryAmount, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO);
                breakdown.setClaimCount((int) details.stream()
                    .filter(d -> d.getCategoryId().equals(entry.getKey())).count());
                analysis.getCategoryBreakdowns().add(breakdown);
            }
        }

        return analysis;
    }

    @Override
    public AnomalyStatistics getAnomalyStats(TrendQuery query) {
        AnomalyStatistics stats = new AnomalyStatistics();

        // 获取所有高风险预警
        LambdaQueryWrapper<RiskAlert> alertWrapper = new LambdaQueryWrapper<>();
        if (query.getStartDate() != null) {
            alertWrapper.ge(RiskAlert::getCreatedAt, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            alertWrapper.le(RiskAlert::getCreatedAt, query.getEndDate().atTime(23, 59, 59));
        }
        alertWrapper.in(RiskAlert::getRiskLevel, 3, 4); // 高风险和严重风险

        List<RiskAlert> alerts = riskAlertMapper.selectList(alertWrapper);
        stats.setTotalAnomalies(alerts.size());

        // 异常类型分布
        Map<String, Long> typeCount = alerts.stream()
            .collect(Collectors.groupingBy(RiskAlert::getRiskType, Collectors.counting()));
        stats.setAnomalyTypeDistribution(typeCount.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue())));

        // 计算异常率
        LambdaQueryWrapper<ExpenseClaim> claimWrapper = new LambdaQueryWrapper<>();
        if (query.getStartDate() != null) {
            claimWrapper.ge(ExpenseClaim::getExpenseDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            claimWrapper.le(ExpenseClaim::getExpenseDate, query.getEndDate());
        }
        long totalClaims = claimMapper.selectCount(claimWrapper);

        if (totalClaims > 0) {
            stats.setAnomalyRate(new BigDecimal(alerts.size())
                .divide(new BigDecimal(totalClaims), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")));
        } else {
            stats.setAnomalyRate(BigDecimal.ZERO);
        }

        return stats;
    }

    private RuleContext buildContext(ExpenseClaim claim) {
        RuleContext context = new RuleContext();
        context.setClaim(claim);
        context.setCurrentDate(LocalDate.now());

        if (claim.getDeptId() != null) {
            context.setDepartment(departmentMapper.selectById(claim.getDeptId()));
        }

        LambdaQueryWrapper<ExpenseClaimDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(ExpenseClaimDetail::getClaimId, claim.getId());
        context.setDetails(detailMapper.selectList(detailWrapper));

        return context;
    }

    private void checkInvoiceIntegrity(ExpenseClaim claim, ComplianceAuditResult result) {
        // 检查是否有明细但没有发票的情况
        // 实际实现需要关联查询
    }

    private String formatPeriod(LocalDate date, String granularity) {
        DateTimeFormatter formatter;
        switch (granularity) {
            case "WEEK":
                formatter = DateTimeFormatter.ofPattern("yyyy-'W'ww");
                break;
            case "MONTH":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                break;
            case "YEAR":
                formatter = DateTimeFormatter.ofPattern("yyyy");
                break;
            default:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        }
        return date.format(formatter);
    }
}
