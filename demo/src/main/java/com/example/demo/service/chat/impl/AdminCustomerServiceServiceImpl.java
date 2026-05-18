package com.example.demo.service.chat.impl;

import com.example.demo.entity.Result;
import com.example.demo.repository.chat.entity.ChatMessage;
import com.example.demo.repository.chat.entity.ChatSession;
import com.example.demo.repository.chat.ChatMessageMapper;
import com.example.demo.repository.chat.ChatSessionMapper;
import com.example.demo.service.chat.AdminCustomerServiceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminCustomerServiceServiceImpl implements AdminCustomerServiceService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public AdminCustomerServiceServiceImpl(ChatSessionMapper chatSessionMapper,
                                           ChatMessageMapper chatMessageMapper,
                                           SimpMessagingTemplate messagingTemplate) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public Result getSessionList(Integer status, Integer page, Integer size, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");
        int offset = (page - 1) * size;
        List<Map<String, Object>> sessions;
        Long total;
        if (status == -1) {
            sessions = chatSessionMapper.getPendingCustomerServiceSessions(offset, size);
            total = chatSessionMapper.countPendingCustomerService();
        } else {
            sessions = chatSessionMapper.getCustomerServiceSessionsByAdmin(adminId, status, offset, size);
            total = chatSessionMapper.countCustomerServiceByAdmin(adminId, status);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("records", sessions);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    @Override
    public Result getStatistics(HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", chatSessionMapper.countPendingCustomerService());
        stats.put("serving", chatSessionMapper.countCustomerServiceByAdmin(adminId, 0));
        stats.put("closed", chatSessionMapper.countCustomerServiceByAdmin(adminId, 1));
        stats.put("unread", chatSessionMapper.countAdminUnread(adminId));
        return Result.success(stats);
    }

    @Override
    public Result acceptSession(Long sessionId, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || session.getSessionType() != 1) {
            return Result.failure("会话不存在");
        }
        if (session.getServiceAdminId() != null) {
            return Result.failure("该会话已被其他客服接入");
        }
        chatSessionMapper.acceptCustomerService(sessionId, adminId);
        ChatMessage sysMsg = new ChatMessage();
        sysMsg.setSessionId(sessionId);
        sysMsg.setSenderId(0L);
        sysMsg.setSenderType(3);
        sysMsg.setContent("客服已接入，请描述您的问题。");
        sysMsg.setMessageType(3);
        sysMsg.setIsRead(0);
        chatMessageMapper.insert(sysMsg);
        chatSessionMapper.incrementCustomerServiceUnread(sessionId, 1, "客服已接入");
        Map<String, Object> msgWithSender = chatMessageMapper.getMessageWithSender(sysMsg.getMessageId());
        messagingTemplate.convertAndSendToUser(session.getCustomerId().toString(), "/queue/customer-service", msgWithSender);
        return Result.success("接入成功");
    }

    @Override
    public Result getMessages(Long sessionId, Integer page, Integer size, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || session.getSessionType() != 1) {
            return Result.failure("会话不存在");
        }
        int offset = (page - 1) * size;
        List<Map<String, Object>> messages = chatMessageMapper.getMessageList(sessionId, offset, size);
        Long total = chatMessageMapper.countBySession(sessionId);
        chatMessageMapper.markAsRead(sessionId, adminId);
        chatSessionMapper.clearCustomerServiceUnread(sessionId, 1);
        Map<String, Object> result = new HashMap<>();
        result.put("records", messages);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("session", session);
        return Result.success(result);
    }

    @Override
    public Result sendMessage(Map<String, Object> params, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");
        Long sessionId = Long.valueOf(params.get("sessionId").toString());
        String content = (String) params.get("content");
        Integer messageType = params.get("messageType") != null ? Integer.valueOf(params.get("messageType").toString()) : 0;
        String fileUrl = (String) params.get("fileUrl");
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || session.getSessionType() != 1) {
            return Result.failure("会话不存在");
        }
        if (session.getServiceAdminId() == null) {
            chatSessionMapper.acceptCustomerService(sessionId, adminId);
            session.setServiceAdminId(adminId);
        }
        if (!session.getServiceAdminId().equals(adminId)) {
            return Result.failure("您不是该会话的客服");
        }
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSenderId(adminId);
        message.setSenderType(3);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setFileUrl(fileUrl);
        message.setIsRead(0);
        chatMessageMapper.insert(message);
        String preview = content.length() > 50 ? content.substring(0, 50) + "..." : content;
        chatSessionMapper.incrementCustomerServiceUnread(sessionId, 1, preview);
        Map<String, Object> messageWithSender = chatMessageMapper.getMessageWithSender(message.getMessageId());
        messagingTemplate.convertAndSendToUser(session.getCustomerId().toString(), "/queue/customer-service", messageWithSender);
        messagingTemplate.convertAndSendToUser(adminId.toString(), "/queue/customer-service", messageWithSender);
        return Result.success(messageWithSender);
    }

    @Override
    public Result closeSession(Long sessionId, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || session.getSessionType() != 1) {
            return Result.failure("会话不存在");
        }
        if (session.getServiceAdminId() != null && !session.getServiceAdminId().equals(adminId)) {
            return Result.failure("您不是该会话的客服");
        }
        chatSessionMapper.closeCustomerService(sessionId);
        ChatMessage sysMsg = new ChatMessage();
        sysMsg.setSessionId(sessionId);
        sysMsg.setSenderId(0L);
        sysMsg.setSenderType(3);
        sysMsg.setContent("本次咨询已结束，感谢您的使用。如有其他问题，欢迎再次咨询。");
        sysMsg.setMessageType(3);
        sysMsg.setIsRead(0);
        chatMessageMapper.insert(sysMsg);
        Map<String, Object> msgWithSender = chatMessageMapper.getMessageWithSender(sysMsg.getMessageId());
        messagingTemplate.convertAndSendToUser(session.getCustomerId().toString(), "/queue/customer-service", msgWithSender);
        return Result.success("会话已结束");
    }

    @Override
    public Result getUnreadCount(HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");
        Long count = chatSessionMapper.countAdminUnread(adminId);
        Long pending = chatSessionMapper.countPendingCustomerService();
        Map<String, Object> result = new HashMap<>();
        result.put("unread", count);
        result.put("pending", pending);
        return Result.success(result);
    }
}
