package com.example.demo.service.chat.impl;

import com.example.demo.entity.Result;
import com.example.demo.repository.chat.entity.ChatMessage;
import com.example.demo.repository.chat.entity.ChatSession;
import com.example.demo.repository.chat.ChatMessageMapper;
import com.example.demo.repository.chat.ChatSessionMapper;
import com.example.demo.service.chat.CustomerServiceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class CustomerServiceServiceImpl implements CustomerServiceService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public CustomerServiceServiceImpl(ChatSessionMapper chatSessionMapper,
                                      ChatMessageMapper chatMessageMapper,
                                      SimpMessagingTemplate messagingTemplate) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public Result createOrGetSession(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ChatSession session = chatSessionMapper.findCustomerServiceSession(userId);
        if (session == null) {
            session = new ChatSession();
            session.setSessionType(1);
            session.setCustomerId(userId);
            session.setServiceAdminId(null);
            session.setUnreadCustomer(0);
            session.setUnreadAdmin(0);
            session.setStatus(0);
            session.setLastMessageTime(LocalDateTime.now());
            session.setHouseId(0L);
            session.setLandlordId(0L);
            session.setTenantId(0L);
            session.setUnreadLandlord(0);
            session.setUnreadTenant(0);
            chatSessionMapper.insert(session);

            ChatMessage welcomeMsg = new ChatMessage();
            welcomeMsg.setSessionId(session.getSessionId());
            welcomeMsg.setSenderId(0L);
            welcomeMsg.setSenderType(3);
            welcomeMsg.setContent("您好！欢迎联系平台客服，请描述您的问题，我们会尽快为您解答。");
            welcomeMsg.setMessageType(3);
            welcomeMsg.setIsRead(0);
            chatMessageMapper.insert(welcomeMsg);

            session.setLastMessage("您好！欢迎联系平台客服...");
            chatSessionMapper.updateById(session);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getSessionId());
        result.put("session", session);
        return Result.success(result);
    }

    @Override
    public Result getSessionDetail(Long sessionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || session.getSessionType() != 1) {
            return Result.failure("会话不存在");
        }
        if (!session.getCustomerId().equals(userId)) {
            return Result.failure("无权访问此会话");
        }
        chatSessionMapper.clearCustomerServiceUnread(sessionId, 0);
        return Result.success(session);
    }

    @Override
    public Result getMessages(Long sessionId, Integer page, Integer size, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || session.getSessionType() != 1) {
            return Result.failure("会话不存在");
        }
        if (!session.getCustomerId().equals(userId)) {
            return Result.failure("无权访问此会话");
        }
        int offset = (page - 1) * size;
        var messages = chatMessageMapper.getMessageList(sessionId, offset, size);
        Long total = chatMessageMapper.countBySession(sessionId);
        chatMessageMapper.markAsRead(sessionId, userId);
        chatSessionMapper.clearCustomerServiceUnread(sessionId, 0);
        Map<String, Object> result = new HashMap<>();
        result.put("records", messages);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    @Override
    public Result sendMessage(Map<String, Object> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer userType = getUserType(request);
        Long sessionId = Long.valueOf(params.get("sessionId").toString());
        String content = (String) params.get("content");
        Integer messageType = params.get("messageType") != null ? Integer.valueOf(params.get("messageType").toString()) : 0;
        String fileUrl = (String) params.get("fileUrl");
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || session.getSessionType() != 1) {
            return Result.failure("会话不存在");
        }
        if (!session.getCustomerId().equals(userId)) {
            return Result.failure("无权发送消息");
        }
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSenderId(userId);
        message.setSenderType(userType);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setFileUrl(fileUrl);
        message.setIsRead(0);
        chatMessageMapper.insert(message);
        String preview = content.length() > 50 ? content.substring(0, 50) + "..." : content;
        chatSessionMapper.incrementCustomerServiceUnread(sessionId, 0, preview);
        Map<String, Object> messageWithSender = chatMessageMapper.getMessageWithSender(message.getMessageId());
        if (session.getServiceAdminId() != null) {
            messagingTemplate.convertAndSendToUser(session.getServiceAdminId().toString(), "/queue/customer-service", messageWithSender);
        }
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/customer-service", messageWithSender);
        return Result.success(messageWithSender);
    }

    @Override
    public Result getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ChatSession session = chatSessionMapper.findCustomerServiceSession(userId);
        if (session == null) {
            return Result.success(0);
        }
        return Result.success(session.getUnreadCustomer() != null ? session.getUnreadCustomer() : 0);
    }

    @Override
    public Result closeSession(Long sessionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || session.getSessionType() != 1) {
            return Result.failure("会话不存在");
        }
        if (!session.getCustomerId().equals(userId)) {
            return Result.failure("无权操作此会话");
        }
        chatSessionMapper.closeCustomerService(sessionId);
        ChatMessage sysMsg = new ChatMessage();
        sysMsg.setSessionId(sessionId);
        sysMsg.setSenderId(0L);
        sysMsg.setSenderType(3);
        sysMsg.setContent("用户已结束本次咨询，感谢使用。");
        sysMsg.setMessageType(3);
        sysMsg.setIsRead(0);
        chatMessageMapper.insert(sysMsg);
        if (session.getServiceAdminId() != null) {
            Map<String, Object> msgWithSender = chatMessageMapper.getMessageWithSender(sysMsg.getMessageId());
            messagingTemplate.convertAndSendToUser(session.getServiceAdminId().toString(), "/queue/customer-service", msgWithSender);
        }
        return Result.success("会话已结束");
    }

    @Override
    public Result getQueuePosition(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ChatSession session = chatSessionMapper.findCustomerServiceSession(userId);
        if (session == null) {
            return Result.success(Map.of("position", 0, "status", "none"));
        }
        if (session.getServiceAdminId() != null) {
            return Result.success(Map.of("position", 0, "status", "serving"));
        }
        Long position = chatSessionMapper.getQueuePosition(session.getSessionId());
        Long totalPending = chatSessionMapper.countPendingCustomerService();
        Map<String, Object> result = new HashMap<>();
        result.put("position", position);
        result.put("total", totalPending);
        result.put("status", "waiting");
        result.put("sessionId", session.getSessionId());
        return Result.success(result);
    }

    private Integer getUserType(HttpServletRequest request) {
        Object userTypeObj = request.getAttribute("userType");
        if (userTypeObj != null) return userTypeObj instanceof Integer ? (Integer) userTypeObj : Integer.valueOf(userTypeObj.toString());
        return 1;
    }
}
