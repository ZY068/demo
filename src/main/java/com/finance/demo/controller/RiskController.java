package com.finance.demo.controller;

import com.finance.demo.common.Result;
import com.finance.demo.entity.RiskAlert;
import com.finance.demo.service.risk.RiskDetectionResult;
import com.finance.demo.service.risk.RiskDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/risk")
@CrossOrigin
public class RiskController {

    @Autowired
    private RiskDetectionService riskDetectionService;

    @PostMapping("/detect")
    public Result<RiskDetectionResult> detectRisk(@RequestBody Map<String, Object> params) {
        Integer claimId = Integer.valueOf(params.get("claimId").toString());
        RiskDetectionResult result = riskDetectionService.detectRisk(claimId);
        return Result.success(result);
    }

    @PostMapping("/batch-detect")
    public Result<List<RiskDetectionResult>> batchDetect(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Integer> claimIds = (List<Integer>) params.get("claimIds");
        List<RiskDetectionResult> results = riskDetectionService.batchDetectRisk(claimIds);
        return Result.success(results);
    }

    @GetMapping("/alerts")
    public Result<List<RiskAlert>> getHighRiskAlerts() {
        List<RiskAlert> alerts = riskDetectionService.getHighRiskAlerts();
        return Result.success(alerts);
    }

    @PutMapping("/alerts/{id}/handle")
    public Result<Void> handleAlert(@PathVariable Integer id, @RequestBody Map<String, Object> params) {
        Integer handlerId = Integer.valueOf(params.get("handlerId").toString());
        String comment = params.getOrDefault("comment", "").toString();
        boolean success = riskDetectionService.handleAlert(id, handlerId, comment);
        return success ? Result.success(null) : Result.error("处理失败");
    }

    /**
     * 获取报销单风险详情
     */
    @GetMapping("/detail/{claimId}")
    public Result<RiskDetectionResult> getRiskDetail(@PathVariable Integer claimId) {
        RiskDetectionResult result = riskDetectionService.getRiskDetail(claimId);
        return Result.success(result);
    }

    /**
     * 重新检测风险
     */
    @PostMapping("/re-detect/{claimId}")
    public Result<RiskDetectionResult> reDetectRisk(@PathVariable Integer claimId) {
        RiskDetectionResult result = riskDetectionService.reDetectRisk(claimId);
        return Result.success(result);
    }
}
