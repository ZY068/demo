package com.finance.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("policy_rule_condition")
public class PolicyRuleCondition {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("rule_id")
    private Integer ruleId;
    @TableField("condition_type")
    private String conditionType;
    private String operator;
    @TableField("field_name")
    private String fieldName;
    @TableField("field_value")
    private String fieldValue;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
