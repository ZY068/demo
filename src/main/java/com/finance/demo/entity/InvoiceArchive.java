package com.finance.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("invoice_archive")
public class InvoiceArchive {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("invoice_id")
    private Integer invoiceId;
    @TableField("file_path")
    private String filePath;
    @TableField("file_hash")
    private String fileHash;
    @TableField("file_size")
    private Long fileSize;
    @TableField("mime_type")
    private String mimeType;
    @TableField("archived_by")
    private Integer archivedBy;
    @TableField("archived_at")
    private LocalDateTime archivedAt;
}
