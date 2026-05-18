package com.example.demo.repository.comment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comment_report")
public class CommentReport {
    @TableId(type = IdType.AUTO)
    private Long reportId;

    private Long commentId;
    private Long reporterId;
    private String reason;
    private Integer status;       // 0待处理 1已通过(删帖) 2已驳回

    @TableField("admin_note")
    private String auditRemark;   // 映射数据库 admin_note 字段

    @TableField(exist = false)
    private Long auditorId;       // 数据库无此字段，忽略

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime handleTime;  // 数据库字段是 handle_time

    @TableField(exist = false)
    private LocalDateTime auditTime;   // 数据库无此字段，忽略
}
