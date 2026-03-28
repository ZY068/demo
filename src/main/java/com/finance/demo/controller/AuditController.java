package com.finance.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finance.demo.common.Result;
import com.finance.demo.dto.*;
import com.finance.demo.entity.AuditLog;
import com.finance.demo.mapper.AuditLogMapper;
import com.finance.demo.service.audit.AuditAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin
public class AuditController {

    @Autowired
    private AuditAnalysisService auditAnalysisService;

    @Autowired
    private AuditLogMapper auditLogMapper;

    /**
     * 获取审计日志列表（管理员使用）
     */
    @GetMapping("/logs")
    public Result<List<AuditLog>> getAuditLogs(
            @RequestParam(required = false) Integer claimId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) Integer operatorId) {
        QueryWrapper<AuditLog> wrapper = new QueryWrapper<>();
        if (claimId != null) {
            wrapper.eq("claim_id", claimId);
        }
        if (actionType != null && !actionType.isEmpty()) {
            wrapper.eq("action_type", actionType);
        }
        if (operatorId != null) {
            wrapper.eq("operator_id", operatorId);
        }
        wrapper.orderByDesc("created_at");
        List<AuditLog> logs = auditLogMapper.selectList(wrapper);
        return Result.success(logs);
    }

    @GetMapping("/compliance/{claimId}")
    public Result<ComplianceAuditResult> auditCompliance(@PathVariable Integer claimId) {
        ComplianceAuditResult result = auditAnalysisService.auditCompliance(claimId);
        return Result.success(result);
    }

    @GetMapping("/process/{claimId}")
    public Result<ProcessAuditResult> auditProcess(@PathVariable Integer claimId) {
        ProcessAuditResult result = auditAnalysisService.auditProcess(claimId);
        return Result.success(result);
    }

    @PostMapping("/reports")
    public Result<String> generateReport(@RequestBody AuditReportRequest request) {
        String reportCode = auditAnalysisService.generateReport(request);
        return Result.success(reportCode);
    }

    @GetMapping("/stats/trend")
    public Result<ExpenseTrendStats> getExpenseTrend(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String granularity,
            @RequestParam(required = false) Integer deptId) {
        TrendQuery query = new TrendQuery();
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setGranularity(granularity);
        query.setDeptId(deptId);
        ExpenseTrendStats stats = auditAnalysisService.getExpenseTrend(query);
        return Result.success(stats);
    }

    @GetMapping("/stats/anomaly")
    public Result<AnomalyStatistics> getAnomalyStats(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Integer deptId) {
        TrendQuery query = new TrendQuery();
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setDeptId(deptId);
        AnomalyStatistics stats = auditAnalysisService.getAnomalyStats(query);
        return Result.success(stats);
    }

    @GetMapping("/stats/department")
    public Result<DepartmentExpenseAnalysis> getDepartmentAnalysis(
            @RequestParam Integer deptId,
            @RequestParam(required = false) Integer year) {
        DepartmentExpenseAnalysis analysis = auditAnalysisService.getDepartmentAnalysis(deptId, year);
        return Result.success(analysis);
    }
}
