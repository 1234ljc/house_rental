package com.example.demo.repository.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.repository.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 获取会话消息列表（带发送者信息）
     */
    @Select("""
        SELECT cm.*, u.username as sender_name, u.avatar as sender_avatar
        FROM chat_message cm
        LEFT JOIN `user` u ON cm.sender_id = u.user_id
        WHERE cm.session_id = #{sessionId}
        ORDER BY cm.create_time ASC
        LIMIT #{offset}, #{size}
    """)
    List<Map<String, Object>> getMessageList(@Param("sessionId") Long sessionId,
                                              @Param("offset") Integer offset,
                                              @Param("size") Integer size);

    /**
     * 统计会话消息数
     */
    @Select("SELECT COUNT(*) FROM chat_message WHERE session_id = #{sessionId}")
    Long countBySession(@Param("sessionId") Long sessionId);

    /**
     * 标记会话中某用户的消息为已读
     */
    @Update("""
        UPDATE chat_message 
        SET is_read = 1 
        WHERE session_id = #{sessionId} 
          AND sender_id != #{userId} 
          AND is_read = 0
    """)
    int markAsRead(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    /**
     * 获取最新消息（用于WebSocket推送后获取完整信息）
     */
    @Select("""
        SELECT cm.*, u.username as sender_name, u.avatar as sender_avatar
        FROM chat_message cm
        LEFT JOIN `user` u ON cm.sender_id = u.user_id
        WHERE cm.message_id = #{messageId}
    """)
    Map<String, Object> getMessageWithSender(@Param("messageId") Long messageId);
}
