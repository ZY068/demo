package com.finance.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("audit_report")
public class AuditReport {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("report_code")
    private String reportCode;
    @TableField("report_type")
    private String reportType;
    @TableField("report_period_start")
    private LocalDate reportPeriodStart;
    @TableField("report_period_end")
    private LocalDate reportPeriodEnd;
    private String summary;
    private String findings;
    private String recommendations;
    @TableField("generated_by")
    private Integer generatedBy;
    private Boolean status;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
