package com.finance.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("department_budget")
public class DepartmentBudget {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("dept_id")
    private Integer deptId;
    @TableField("budget_year")
    private Integer budgetYear;
    @TableField("budget_amount")
    private BigDecimal budgetAmount;
    @TableField("used_amount")
    private BigDecimal usedAmount;
    @TableField("frozen_amount")
    private BigDecimal frozenAmount;
    @Version
    private Integer version;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
