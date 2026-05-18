package com.example.demo.service.chat;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface CustomerServiceService{
    Result createOrGetSession(HttpServletRequest request);

    Result getSessionDetail(Long sessionId, HttpServletRequest request);

    Result getMessages(Long sessionId, Integer page, Integer size, HttpServletRequest request);

    Result sendMessage(Map<String, Object> params, HttpServletRequest request);

    Result getUnreadCount(HttpServletRequest request);

    Result closeSession(Long sessionId, HttpServletRequest request);

    Result getQueuePosition(HttpServletRequest request);
}
