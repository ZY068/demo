package com.finance.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("expense_policy_rule")
public class ExpensePolicyRule {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("rule_code")
    private String ruleCode;
    @TableField("rule_name")
    private String ruleName;
    @TableField("rule_type")
    private String ruleType;
    private String description;
    private Integer priority;
    private Boolean enabled;
    @TableField("error_message")
    private String errorMessage;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
