package com.finance.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("expense_claim_detail")
public class ExpenseClaimDetail {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("claim_id")
    private Integer claimId;
    @TableField("category_id")
    private Integer categoryId;
    private String description;
    private BigDecimal amount;
    @TableField("invoice_id")
    private Integer invoiceId;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
