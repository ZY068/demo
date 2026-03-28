package com.finance.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("invoice")
public class Invoice {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("invoice_number")
    private String invoiceNumber;
    @TableField("invoice_code")
    private String invoiceCode;
    @TableField("invoice_type")
    private String invoiceType;
    private BigDecimal amount;
    @TableField("tax_amount")
    private BigDecimal taxAmount;
    @TableField("total_amount")
    private BigDecimal totalAmount;
    @TableField("issue_date")
    private LocalDate issueDate;
    @TableField("seller_name")
    private String sellerName;
    @TableField("seller_tax_no")
    private String sellerTaxNo;
    @TableField("buyer_name")
    private String buyerName;
    @TableField("buyer_tax_no")
    private String buyerTaxNo;
    @TableField("ocr_raw_text")
    private String ocrRawText;
    @TableField("ocr_confidence")
    private BigDecimal ocrConfidence;
    @TableField("ocr_status")
    private Integer ocrStatus;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
