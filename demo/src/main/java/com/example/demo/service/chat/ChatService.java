package com.example.demo.service.chat;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

public interface ChatService{
    Result getSessionList(Integer page, Integer size, HttpServletRequest request);

    Result getUnreadCount(HttpServletRequest request);

    Result createOrGetSession(Map<String, Long> params, HttpServletRequest request);

    Result getSessionDetail(Long sessionId, HttpServletRequest request);

    Result getMessages(Long sessionId, Integer page, Integer size, HttpServletRequest request);

    Result sendMessage(Map<String, Object> params, HttpServletRequest request);

    void handleWebSocketMessage(Map<String, Object> payload, Principal principal);

    Result markAsRead(Long sessionId, HttpServletRequest request);

    Result uploadFile(MultipartFile file, Long sessionId, String type, HttpServletRequest request);

    void downloadFile(Long sessionId, String fileName, String name, HttpServletRequest request, HttpServletResponse response) throws IOException;
}
