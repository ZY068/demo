package com.finance.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("risk_rule_config")
public class RiskRuleConfig {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("rule_code")
    private String ruleCode;
    @TableField("rule_name")
    private String ruleName;
    @TableField("risk_type")
    private String riskType;
    @TableField("risk_level")
    private Integer riskLevel;
    private BigDecimal weight;
    @TableField("condition_expression")
    private String conditionExpression;
    private Boolean enabled;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
