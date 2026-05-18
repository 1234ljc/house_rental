package com.example.demo.repository.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long notifyId;
    
    private Long userId;          // 接收用户ID
    private Integer notifyType;   // 通知类型：1系统通知 4合同通知 5支付通知 6问题反馈
    private String title;         // 标题
    private String content;       // 内容
    private Long relatedId;       // 关联业务ID
    private Integer isRead;       // 是否已读：0未读 1已读
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
