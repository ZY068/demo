package com.finance.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("approval_flow")
public class ApprovalFlow {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("flow_code")
    private String flowCode;
    @TableField("flow_name")
    private String flowName;
    @TableField("flow_type")
    private String flowType;
    @TableField("condition_expression")
    private String conditionExpression;
    private Boolean enabled;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
