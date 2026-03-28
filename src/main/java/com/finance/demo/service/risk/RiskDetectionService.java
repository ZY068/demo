package com.finance.demo.service.risk;

import com.finance.demo.entity.RiskAlert;

import java.util.List;

public interface RiskDetectionService {
    /**
     * 检测报销单风险
     */
    RiskDetectionResult detectRisk(Integer claimId);

    /**
     * 异步检测报销单风险（不阻塞主流程）
     */
    void detectRiskAsync(Integer claimId);

    /**
     * 重新检测风险（清除旧预警，重新计算）
     */
    RiskDetectionResult reDetectRisk(Integer claimId);

    /**
     * 获取报销单风险详情
     */
    RiskDetectionResult getRiskDetail(Integer claimId);

    /**
     * 批量检测风险
     */
    List<RiskDetectionResult> batchDetectRisk(List<Integer> claimIds);

    /**
     * 获取高风险预警列表
     */
    List<RiskAlert> getHighRiskAlerts();

    /**
     * 处理风险预警
     */
    boolean handleAlert(Integer alertId, Integer handlerId, String comment);
}
