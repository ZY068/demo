package com.finance.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审计日志实体
 * 记录所有操作的时间痕迹和修改痕迹
 */
@Data
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer claimId;

    private Integer operatorId;

    /**
     * 动作类型：SUBMIT/APPROVE/REJECT/AUDIT/MODIFY/DELETE
     */
    private String actionType;

    /**
     * 操作描述
     */
    private String action;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 设备信息(User-Agent)
     */
    private String deviceInfo;

    /**
     * 变更前值(JSON格式)
     */
    private String beforeValue;

    /**
     * 变更后值(JSON格式)
     */
    private String afterValue;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}