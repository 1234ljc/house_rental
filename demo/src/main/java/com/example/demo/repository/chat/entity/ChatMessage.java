package com.example.demo.repository.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息实体
 */
@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long messageId;
    
    private Long sessionId;         // 会话ID
    private Long senderId;          // 发送者ID
    private Integer senderType;     // 发送者类型：1租客 2房东
    private String content;         // 消息内容
    private Integer messageType;    // 消息类型：0文本 1图片 2文件 3系统消息
    private String fileUrl;         // 文件/图片URL
    private Integer isRead;         // 是否已读：0未读 1已读
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
