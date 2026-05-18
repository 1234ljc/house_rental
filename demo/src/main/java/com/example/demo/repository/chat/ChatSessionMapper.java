package com.example.demo.repository.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.repository.chat.entity.ChatSession;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /**
     * 获取用户的会话列表（带房源和对方用户信息）- 只查普通会话
     */
    @Select("""
        SELECT cs.*, 
               h.title as house_title, h.images as house_images, h.address as house_address,
               CASE WHEN #{userType} = 1 THEN u_landlord.user_id ELSE u_tenant.user_id END as other_user_id,
               CASE WHEN #{userType} = 1 THEN u_landlord.username ELSE u_tenant.username END as other_username,
               CASE WHEN #{userType} = 1 THEN u_landlord.avatar ELSE u_tenant.avatar END as other_avatar,
               CASE WHEN #{userType} = 1 THEN cs.unread_tenant ELSE cs.unread_landlord END as unread_count
        FROM chat_session cs
        LEFT JOIN house h ON cs.house_id = h.house_id
        LEFT JOIN `user` u_landlord ON cs.landlord_id = u_landlord.user_id
        LEFT JOIN `user` u_tenant ON cs.tenant_id = u_tenant.user_id
        WHERE (cs.session_type = 0 OR cs.session_type IS NULL)
          AND ((#{userType} = 1 AND cs.tenant_id = #{userId}) 
           OR (#{userType} = 2 AND cs.landlord_id = #{userId}))
        ORDER BY cs.last_message_time DESC
        LIMIT #{offset}, #{size}
    """)
    List<Map<String, Object>> getSessionListByUser(@Param("userId") Long userId, 
                                                    @Param("userType") Integer userType,
                                                    @Param("offset") Integer offset, 
                                                    @Param("size") Integer size);

    /**
     * 统计用户会话数（只统计普通会话）
     */
    @Select("""
        SELECT COUNT(*) FROM chat_session 
        WHERE (session_type = 0 OR session_type IS NULL)
          AND ((#{userType} = 1 AND tenant_id = #{userId}) 
           OR (#{userType} = 2 AND landlord_id = #{userId}))
    """)
    Long countByUser(@Param("userId") Long userId, @Param("userType") Integer userType);

    /**
     * 统计用户总未读消息数（只统计普通会话）
     */
    @Select("""
        SELECT COALESCE(SUM(CASE WHEN #{userType} = 1 THEN unread_tenant ELSE unread_landlord END), 0)
        FROM chat_session 
        WHERE (session_type = 0 OR session_type IS NULL)
          AND ((#{userType} = 1 AND tenant_id = #{userId}) 
           OR (#{userType} = 2 AND landlord_id = #{userId}))
    """)
    Long countTotalUnread(@Param("userId") Long userId, @Param("userType") Integer userType);

    /**
     * 清空某方未读数
     */
    @Update("""
        UPDATE chat_session 
        SET unread_landlord = CASE WHEN #{userType} = 2 THEN 0 ELSE unread_landlord END,
            unread_tenant = CASE WHEN #{userType} = 1 THEN 0 ELSE unread_tenant END
        WHERE session_id = #{sessionId}
    """)
    int clearUnread(@Param("sessionId") Long sessionId, @Param("userType") Integer userType);

    /**
     * 增加对方未读数
     */
    @Update("""
        UPDATE chat_session 
        SET unread_landlord = unread_landlord + CASE WHEN #{senderType} = 1 THEN 1 ELSE 0 END,
            unread_tenant = unread_tenant + CASE WHEN #{senderType} = 2 THEN 1 ELSE 0 END,
            last_message = #{lastMessage},
            last_message_time = NOW()
        WHERE session_id = #{sessionId}
    """)
    int incrementUnread(@Param("sessionId") Long sessionId, 
                        @Param("senderType") Integer senderType,
                        @Param("lastMessage") String lastMessage);

    /**
     * 查找已存在的会话
     */
    @Select("""
        SELECT * FROM chat_session 
        WHERE house_id = #{houseId} AND landlord_id = #{landlordId} AND tenant_id = #{tenantId}
        LIMIT 1
    """)
    ChatSession findExistingSession(@Param("houseId") Long houseId, 
                                    @Param("landlordId") Long landlordId, 
                                    @Param("tenantId") Long tenantId);

    // ==================== 客服会话相关方法 ====================

    /**
     * 查找用户的客服会话（进行中）
     */
    @Select("""
        SELECT * FROM chat_session 
        WHERE session_type = 1 AND customer_id = #{customerId} AND status = 0
        ORDER BY create_time DESC
        LIMIT 1
    """)
    ChatSession findCustomerServiceSession(@Param("customerId") Long customerId);

    /**
     * 查找用户已结束的客服会话（用于重新激活）
     */
    @Select("""
        SELECT * FROM chat_session 
        WHERE session_type = 1 AND customer_id = #{customerId} AND status = 1
        ORDER BY update_time DESC
        LIMIT 1
    """)
    ChatSession findClosedCustomerServiceSession(@Param("customerId") Long customerId);

    /**
     * 获取用户的客服会话列表
     */
    @Select("""
        SELECT cs.*, 
               u_admin.username as admin_username, u_admin.avatar as admin_avatar,
               cs.unread_customer as unread_count
        FROM chat_session cs
        LEFT JOIN `user` u_admin ON cs.service_admin_id = u_admin.user_id
        WHERE cs.session_type = 1 AND cs.customer_id = #{customerId}
        ORDER BY cs.last_message_time DESC
        LIMIT #{offset}, #{size}
    """)
    List<Map<String, Object>> getCustomerServiceSessionsByUser(@Param("customerId") Long customerId,
                                                                @Param("offset") Integer offset,
                                                                @Param("size") Integer size);

    /**
     * 获取管理员的客服会话列表（服务中/已结束，只查已分配给该客服的）
     */
    @Select("""
        SELECT cs.*, 
               u_customer.username as customer_username, 
               u_customer.avatar as customer_avatar,
               u_customer.user_type as customer_type,
               cs.unread_admin as unread_count
        FROM chat_session cs
        LEFT JOIN `user` u_customer ON cs.customer_id = u_customer.user_id
        WHERE cs.session_type = 1 
          AND cs.service_admin_id = #{adminId}
          AND cs.status = #{status}
        ORDER BY cs.last_message_time DESC
        LIMIT #{offset}, #{size}
    """)
    List<Map<String, Object>> getCustomerServiceSessionsByAdmin(@Param("adminId") Long adminId,
                                                                 @Param("status") Integer status,
                                                                 @Param("offset") Integer offset,
                                                                 @Param("size") Integer size);

    /**
     * 获取待接入的客服会话列表（按等待时间排序，最久的排最前）
     */
    @Select("""
        SELECT cs.*, 
               u_customer.username as customer_username, 
               u_customer.avatar as customer_avatar,
               u_customer.user_type as customer_type,
               cs.unread_admin as unread_count
        FROM chat_session cs
        LEFT JOIN `user` u_customer ON cs.customer_id = u_customer.user_id
        WHERE cs.session_type = 1 
          AND cs.service_admin_id IS NULL
          AND cs.status = 0
        ORDER BY cs.create_time ASC
        LIMIT #{offset}, #{size}
    """)
    List<Map<String, Object>> getPendingCustomerServiceSessions(@Param("offset") Integer offset,
                                                                 @Param("size") Integer size);

    /**
     * 统计管理员客服会话数（只统计已分配给该客服的）
     */
    @Select("""
        SELECT COUNT(*) FROM chat_session 
        WHERE session_type = 1 
          AND service_admin_id = #{adminId}
          AND status = #{status}
    """)
    Long countCustomerServiceByAdmin(@Param("adminId") Long adminId, @Param("status") Integer status);

    /**
     * 统计待接入的客服会话数
     */
    @Select("""
        SELECT COUNT(*) FROM chat_session 
        WHERE session_type = 1 AND service_admin_id IS NULL AND status = 0
    """)
    Long countPendingCustomerService();

    /**
     * 获取用户在排队中的位置（按创建时间排序，比当前会话更早的待接入会话数+1）
     */
    @Select("""
        SELECT COUNT(*) + 1 FROM chat_session 
        WHERE session_type = 1 AND service_admin_id IS NULL AND status = 0
          AND create_time < (SELECT create_time FROM chat_session WHERE session_id = #{sessionId})
    """)
    Long getQueuePosition(@Param("sessionId") Long sessionId);

    /**
     * 统计管理员未读客服消息数
     */
    @Select("""
        SELECT COALESCE(SUM(unread_admin), 0) FROM chat_session 
        WHERE session_type = 1 AND service_admin_id = #{adminId} AND status = 0
    """)
    Long countAdminUnread(@Param("adminId") Long adminId);

    /**
     * 清空客服会话未读数
     */
    @Update("""
        UPDATE chat_session 
        SET unread_customer = CASE WHEN #{isAdmin} = 0 THEN 0 ELSE unread_customer END,
            unread_admin = CASE WHEN #{isAdmin} = 1 THEN 0 ELSE unread_admin END
        WHERE session_id = #{sessionId}
    """)
    int clearCustomerServiceUnread(@Param("sessionId") Long sessionId, @Param("isAdmin") Integer isAdmin);

    /**
     * 增加客服会话未读数
     */
    @Update("""
        UPDATE chat_session 
        SET unread_customer = unread_customer + CASE WHEN #{isFromAdmin} = 1 THEN 1 ELSE 0 END,
            unread_admin = unread_admin + CASE WHEN #{isFromAdmin} = 0 THEN 1 ELSE 0 END,
            last_message = #{lastMessage},
            last_message_time = NOW()
        WHERE session_id = #{sessionId}
    """)
    int incrementCustomerServiceUnread(@Param("sessionId") Long sessionId,
                                        @Param("isFromAdmin") Integer isFromAdmin,
                                        @Param("lastMessage") String lastMessage);

    /**
     * 接入客服会话
     */
    @Update("""
        UPDATE chat_session 
        SET service_admin_id = #{adminId}
        WHERE session_id = #{sessionId} AND session_type = 1
    """)
    int acceptCustomerService(@Param("sessionId") Long sessionId, @Param("adminId") Long adminId);

    /**
     * 关闭客服会话
     */
    @Update("""
        UPDATE chat_session 
        SET status = 1
        WHERE session_id = #{sessionId} AND session_type = 1
    """)
    int closeCustomerService(@Param("sessionId") Long sessionId);

    /**
     * 统计指定房源的咨询量
     */
    @Select("SELECT COUNT(*) FROM chat_session WHERE house_id = #{houseId} AND (session_type = 0 OR session_type IS NULL)")
    Long countConsultByHouseId(@Param("houseId") Long houseId);
}
