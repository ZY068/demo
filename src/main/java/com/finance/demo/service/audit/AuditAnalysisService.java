package com.finance.demo.service.audit;

import com.finance.demo.dto.*;

public interface AuditAnalysisService {
    /**
     * 合规审计
     */
    ComplianceAuditResult auditCompliance(Integer claimId);

    /**
     * 流程审计
     */
    ProcessAuditResult auditProcess(Integer claimId);

    /**
     * 生成审计报告
     */
    String generateReport(AuditReportRequest request);

    /**
     * 获取报销趋势统计
     */
    ExpenseTrendStats getExpenseTrend(TrendQuery query);

    /**
     * 获取部门费用分析
     */
    DepartmentExpenseAnalysis getDepartmentAnalysis(Integer deptId, Integer year);

    /**
     * 获取异常统计
     */
    AnomalyStatistics getAnomalyStats(TrendQuery query);
}
