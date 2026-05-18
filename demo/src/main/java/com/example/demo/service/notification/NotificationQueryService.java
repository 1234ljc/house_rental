package com.example.demo.service.notification;

import com.example.demo.entity.Result;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface NotificationQueryService{
    Result getUnreadCount(HttpServletRequest request);

    Result getNotificationList(Integer notifyType, Integer isRead, Integer page, Integer size, HttpServletRequest request);

    Result getNotificationStats(HttpServletRequest request);

    Result markAsRead(Long notifyId, HttpServletRequest request);

    Result markAllAsRead(HttpServletRequest request);

    Result deleteNotification(Long notifyId, HttpServletRequest request);

    Result clearReadNotifications(HttpServletRequest request);
}
