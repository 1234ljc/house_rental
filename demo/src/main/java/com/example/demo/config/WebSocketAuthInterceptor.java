package com.example.demo.config;

import com.example.demo.utils.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket认证拦截器
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final RedisCache redisCache;

    public WebSocketAuthInterceptor(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 获取token
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                
                // 从Redis验证token
                Object userInfoObj = redisCache.getToken(token);
                if (userInfoObj != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userInfo = (Map<String, Object>) userInfoObj;
                    Object userIdObj = userInfo.get("userId");
                    Object userTypeObj = userInfo.get("userType");
                    
                    if (userIdObj != null) {
                        Long userId;
                        if (userIdObj instanceof Integer) {
                            userId = ((Integer) userIdObj).longValue();
                        } else if (userIdObj instanceof Long) {
                            userId = (Long) userIdObj;
                        } else {
                            userId = Long.valueOf(userIdObj.toString());
                        }
                        
                        Integer userType = userTypeObj != null ? 
                            (userTypeObj instanceof Integer ? (Integer) userTypeObj : Integer.valueOf(userTypeObj.toString())) : 1;
                        
                        // 设置用户身份
                        accessor.setUser(new ChatPrincipal(userId, userType));
                        log.info("WebSocket用户认证成功: userId={}, userType={}", userId, userType);
                    }
                }
            }
        }
        return message;
    }

    /**
     * 自定义Principal实现
     */
    public static class ChatPrincipal implements Principal {
        private final Long userId;
        private final Integer userType;

        public ChatPrincipal(Long userId, Integer userType) {
            this.userId = userId;
            this.userType = userType;
        }

        @Override
        public String getName() {
            return userId.toString();
        }

        public Long getUserId() {
            return userId;
        }

        public Integer getUserType() {
            return userType;
        }
    }
}
