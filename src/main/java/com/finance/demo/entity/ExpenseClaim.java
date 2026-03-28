package com.finance.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("expense_claim")
public class ExpenseClaim {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String claimNo;
    private Integer userId;
    private Integer deptId;
    private String title;
    private String description;
    private Long amount;
    @TableField("expense_date")
    private LocalDate expenseDate;
    private Integer status;
    @TableField("audit_comment")
    private String auditComment;
    @TableField("risk_score")
    private BigDecimal riskScore;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
