package com.example.demo.repository.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天会话实体
 */
@Data
@TableName("chat_session")
public class ChatSession {
    @TableId(type = IdType.AUTO)
    private Long sessionId;
    
    private Long houseId;           // 关联房源ID（普通会话用）
    private Long landlordId;        // 房东ID（普通会话用）
    private Long tenantId;          // 租客ID（普通会话用）
    private String lastMessage;     // 最后一条消息预览
    private LocalDateTime lastMessageTime;  // 最后消息时间
    private Integer unreadLandlord; // 房东未读数
    private Integer unreadTenant;   // 租客未读数
    private Integer status;         // 状态：0正常 1已关闭
    
    // 客服会话相关字段
    private Integer sessionType;    // 会话类型：0普通会话 1客服会话
    private Long customerId;        // 客户ID（发起客服咨询的用户）
    private Long serviceAdminId;    // 客服管理员ID
    private Integer unreadCustomer; // 客户未读数
    private Integer unreadAdmin;    // 客服未读数
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
