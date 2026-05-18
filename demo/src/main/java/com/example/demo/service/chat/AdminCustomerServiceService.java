package com.example.demo.service.chat;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface AdminCustomerServiceService{
    Result getSessionList(Integer status, Integer page, Integer size, HttpServletRequest request);

    Result getStatistics(HttpServletRequest request);

    Result acceptSession(Long sessionId, HttpServletRequest request);

    Result getMessages(Long sessionId, Integer page, Integer size, HttpServletRequest request);

    Result sendMessage(Map<String, Object> params, HttpServletRequest request);

    Result closeSession(Long sessionId, HttpServletRequest request);

    Result getUnreadCount(HttpServletRequest request);
}
