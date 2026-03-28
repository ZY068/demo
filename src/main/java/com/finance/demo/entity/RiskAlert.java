package com.finance.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("risk_alert")
public class RiskAlert {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("claim_id")
    private Integer claimId;
    @TableField("risk_type")
    private String riskType;
    @TableField("risk_level")
    private Integer riskLevel;
    @TableField("risk_score")
    private BigDecimal riskScore;
    @TableField("alert_message")
    private String alertMessage;
    @TableField("trigger_rules")
    private String triggerRules;
    @TableField("handling_status")
    private Integer handlingStatus;
    @TableField("handled_by")
    private Integer handledBy;
    @TableField("handled_at")
    private LocalDateTime handledAt;
    @TableField("handled_comment")
    private String handledComment;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
