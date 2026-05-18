package com.example.demo.service.chat.impl;

import com.example.demo.config.WebSocketAuthInterceptor.ChatPrincipal;
import com.example.demo.entity.Result;
import com.example.demo.repository.chat.entity.ChatMessage;
import com.example.demo.repository.chat.entity.ChatSession;
import com.example.demo.repository.chat.ChatMessageMapper;
import com.example.demo.repository.chat.ChatSessionMapper;
import com.example.demo.service.chat.ChatService;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.house.HouseMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final HouseMapper houseMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public ChatServiceImpl(ChatSessionMapper chatSessionMapper,
                           ChatMessageMapper chatMessageMapper,
                           HouseMapper houseMapper,
                           SimpMessagingTemplate messagingTemplate) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.houseMapper = houseMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    // 获取当前用户的聊天会话列表，返回分页数据给前端会话页展示。
    public Result getSessionList(Integer page, Integer size, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer userType = getUserType(request);
        int offset = (page - 1) * size;
        List<Map<String, Object>> sessions = chatSessionMapper.getSessionListByUser(userId, userType, offset, size);
        Long total = chatSessionMapper.countByUser(userId, userType);
        Map<String, Object> result = new HashMap<>();
        result.put("records", sessions);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    @Override
    // 统计当前用户所有聊天会话里的未读消息总数，用于消息角标提醒。
    public Result getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer userType = getUserType(request);
        Long count = chatSessionMapper.countTotalUnread(userId, userType);
        return Result.success(count);
    }

    @Override
    // 根据房源和双方身份创建聊天会话；如果已存在则直接返回，避免重复建会话。
    public Result createOrGetSession(Map<String, Long> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer userType = getUserType(request);
        Long houseId = params.get("houseId");
        if (houseId == null) {
            return Result.failure("房源ID不能为空");
        }
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }
        Long landlordId, tenantId;
        if (userType == 1) {
            tenantId = userId;
            landlordId = house.getLandlordId();
        } else {
            landlordId = userId;
            tenantId = params.get("tenantId");
            if (tenantId == null) {
                return Result.failure("租客ID不能为空");
            }
        }
        ChatSession session = chatSessionMapper.findExistingSession(houseId, landlordId, tenantId);
        if (session == null) {
            session = new ChatSession();
            session.setHouseId(houseId);
            session.setLandlordId(landlordId);
            session.setTenantId(tenantId);
            session.setUnreadLandlord(0);
            session.setUnreadTenant(0);
            session.setStatus(0);
            session.setLastMessageTime(LocalDateTime.now());
            chatSessionMapper.insert(session);
        }
        return Result.success(session);
    }

    @Override
    public Result getSessionDetail(Long sessionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer userType = getUserType(request);
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            return Result.failure("会话不存在");
        }
        if (!session.getLandlordId().equals(userId) && !session.getTenantId().equals(userId)) {
            return Result.failure("无权访问此会话");
        }
        chatSessionMapper.clearUnread(sessionId, userType);
        House house = houseMapper.selectById(session.getHouseId());
        Map<String, Object> result = new HashMap<>();
        result.put("session", session);
        result.put("house", house);
        return Result.success(result);
    }

    @Override
    // 拉取某个会话的消息记录，并顺便把该会话的未读状态清零。
    public Result getMessages(Long sessionId, Integer page, Integer size, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer userType = getUserType(request);
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            return Result.failure("会话不存在");
        }
        if (!session.getLandlordId().equals(userId) && !session.getTenantId().equals(userId)) {
            return Result.failure("无权访问此会话");
        }
        int offset = (page - 1) * size;
        List<Map<String, Object>> messages = chatMessageMapper.getMessageList(sessionId, offset, size);
        Long total = chatMessageMapper.countBySession(sessionId);
        chatMessageMapper.markAsRead(sessionId, userId);
        chatSessionMapper.clearUnread(sessionId, userType);
        Map<String, Object> result = new HashMap<>();
        result.put("records", messages);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    @Override
    // 聊天中心核心流程：校验会话权限、保存消息、更新未读数，并通过 WebSocket 实时推送给双方。
    public Result sendMessage(Map<String, Object> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer userType = getUserType(request);
        Long sessionId = Long.valueOf(params.get("sessionId").toString());
        String content = (String) params.get("content");
        Integer messageType = params.get("messageType") != null ? Integer.valueOf(params.get("messageType").toString()) : 0;
        String fileUrl = (String) params.get("fileUrl");
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            return Result.failure("会话不存在");
        }
        if (!session.getLandlordId().equals(userId) && !session.getTenantId().equals(userId)) {
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
        chatSessionMapper.incrementUnread(sessionId, userType, preview);
        Map<String, Object> messageWithSender = chatMessageMapper.getMessageWithSender(message.getMessageId());
        messagingTemplate.convertAndSendToUser(session.getLandlordId().toString(), "/queue/messages", messageWithSender);
        messagingTemplate.convertAndSendToUser(session.getTenantId().toString(), "/queue/messages", messageWithSender);
        return Result.success(messageWithSender);
    }

    @Override
    public void handleWebSocketMessage(Map<String, Object> payload, Principal principal) {
        if (principal == null) {
            log.warn("WebSocket消息发送失败：用户未认证");
            return;
        }
        ChatPrincipal chatPrincipal = (ChatPrincipal) principal;
        Long userId = chatPrincipal.getUserId();
        Integer userType = chatPrincipal.getUserType();
        Long sessionId = Long.valueOf(payload.get("sessionId").toString());
        String content = (String) payload.get("content");
        Integer messageType = payload.get("messageType") != null ? Integer.valueOf(payload.get("messageType").toString()) : 0;
        String fileUrl = (String) payload.get("fileUrl");
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || (!session.getLandlordId().equals(userId) && !session.getTenantId().equals(userId))) {
            log.warn("WebSocket消息发送失败：无权访问会话 sessionId={}", sessionId);
            return;
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
        chatSessionMapper.incrementUnread(sessionId, userType, preview);
        Map<String, Object> messageWithSender = chatMessageMapper.getMessageWithSender(message.getMessageId());
        messagingTemplate.convertAndSendToUser(session.getLandlordId().toString(), "/queue/messages", messageWithSender);
        messagingTemplate.convertAndSendToUser(session.getTenantId().toString(), "/queue/messages", messageWithSender);
        log.info("WebSocket消息发送成功: sessionId={}, senderId={}", sessionId, userId);
    }

    @Override
    public Result markAsRead(Long sessionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer userType = getUserType(request);
        chatMessageMapper.markAsRead(sessionId, userId);
        chatSessionMapper.clearUnread(sessionId, userType);
        return Result.success();
    }

    @Override
    public Result uploadFile(MultipartFile file, Long sessionId, String type, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer userType = getUserType(request);
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            return Result.failure("会话不存在");
        }
        boolean hasPermission = session.getSessionType() != null && session.getSessionType() == 1 ? (userId.equals(session.getCustomerId()) || userType == 3) : (userId.equals(session.getLandlordId()) || userId.equals(session.getTenantId()));
        if (!hasPermission) {
            return Result.failure("无权操作");
        }
        if (file.isEmpty()) {
            return Result.failure("文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase() : "";
        int messageType;
        if ("image".equals(type)) {
            if (!Arrays.asList("jpg", "jpeg", "png", "gif", "webp").contains(fileExtension)) {
                return Result.failure("不支持的图片格式");
            }
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.failure("图片大小不能超过5MB");
            }
            messageType = 1;
        } else {
            if (file.getSize() > 20 * 1024 * 1024) {
                return Result.failure("文件大小不能超过20MB");
            }
            messageType = 2;
        }
        try {
            Path chatFilePath = Paths.get(uploadDir, "chat", sessionId.toString());
            if (!Files.exists(chatFilePath)) {
                Files.createDirectories(chatFilePath);
            }
            String newFileName = UUID.randomUUID().toString() + "." + fileExtension;
            Path filePath = chatFilePath.resolve(newFileName);
            Files.copy(file.getInputStream(), filePath);
            String fileUrl = "/uploads/chat/" + sessionId + "/" + newFileName + "?name=" + java.net.URLEncoder.encode(originalFilename, "UTF-8");
            ChatMessage message = new ChatMessage();
            message.setSessionId(sessionId);
            message.setSenderId(userId);
            message.setSenderType(userType);
            message.setContent(originalFilename);
            message.setMessageType(messageType);
            message.setFileUrl(fileUrl);
            message.setIsRead(0);
            chatMessageMapper.insert(message);
            Map<String, Object> messageWithSender = chatMessageMapper.getMessageWithSender(message.getMessageId());
            if (session.getSessionType() != null && session.getSessionType() == 1) {
                int isFromAdmin = userType == 3 ? 1 : 0;
                chatSessionMapper.incrementCustomerServiceUnread(sessionId, isFromAdmin, messageType == 1 ? "[图片]" : "[文件] " + originalFilename);
                messagingTemplate.convertAndSendToUser(session.getCustomerId().toString(), "/queue/customer-service", messageWithSender);
                if (session.getServiceAdminId() != null) {
                    messagingTemplate.convertAndSendToUser(session.getServiceAdminId().toString(), "/queue/customer-service", messageWithSender);
                }
            } else {
                chatSessionMapper.incrementUnread(sessionId, userType, messageType == 1 ? "[图片]" : "[文件] " + originalFilename);
                messagingTemplate.convertAndSendToUser(session.getLandlordId().toString(), "/queue/messages", messageWithSender);
                messagingTemplate.convertAndSendToUser(session.getTenantId().toString(), "/queue/messages", messageWithSender);
            }
            return Result.success(messageWithSender);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.failure("文件上传失败");
        }
    }

    @Override
    public void downloadFile(Long sessionId, String fileName, String name, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long userId = (Long) request.getAttribute("userId");
        Integer userType = getUserType(request);
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            response.sendError(404, "会话不存在");
            return;
        }
        boolean hasPermission = session.getSessionType() != null && session.getSessionType() == 1 ? (userId.equals(session.getCustomerId()) || userType == 3) : (userId.equals(session.getLandlordId()) || userId.equals(session.getTenantId()));
        if (!hasPermission) {
            response.sendError(403, "无权访问");
            return;
        }
        Path filePath = Paths.get(uploadDir, "chat", sessionId.toString(), fileName);
        if (!Files.exists(filePath)) {
            response.sendError(404, "文件不存在");
            return;
        }
        String downloadName = name != null ? name : fileName;
        String encodedFileName = java.net.URLEncoder.encode(downloadName, "UTF-8").replaceAll("\\+", "%20");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setContentLengthLong(Files.size(filePath));
        Files.copy(filePath, response.getOutputStream());
    }

    private Integer getUserType(HttpServletRequest request) {
        Object userTypeObj = request.getAttribute("userType");
        if (userTypeObj != null) return userTypeObj instanceof Integer ? (Integer) userTypeObj : Integer.valueOf(userTypeObj.toString());
        return 1;
    }
}
