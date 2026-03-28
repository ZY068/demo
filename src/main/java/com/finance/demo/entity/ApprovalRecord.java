package com.finance.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审批记录实体
 * 记录审批痕迹：谁、在什么时间、审批了什么、结果是什么
 */
@Data
@TableName("approval_record")
public class ApprovalRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("claim_id")
    private Integer claimId;

    @TableField("flow_id")
    private Integer flowId;

    @TableField("approver_id")
    private Integer approverId;

    @TableField("approval_level")
    private Integer approvalLevel;

    /**
     * 审批阶段：1-部门经理 2-财务审计
     */
    @TableField("approval_stage")
    private Integer approvalStage;

    /**
     * 审批前状态
     */
    @TableField("before_status")
    private Integer beforeStatus;

    /**
     * 审批后状态
     */
    @TableField("after_status")
    private Integer afterStatus;

    /**
     * 动作：APPROVE/REJECT/TRANSFER
     */
    private String action;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 审批IP
     */
    @TableField("ip_address")
    private String ipAddress;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}