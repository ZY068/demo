package com.finance.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("department")
public class Department {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("dept_code")
    private String deptCode;
    @TableField("dept_name")
    private String deptName;
    @TableField("parent_id")
    private Integer parentId;
    @TableField("manager_id")
    private Integer managerId;
    private Boolean status;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
