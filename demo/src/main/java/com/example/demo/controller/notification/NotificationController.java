package com.example.demo.controller.notification;

import com.example.demo.entity.Result;
import com.example.demo.service.notification.NotificationQueryService;

import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    public NotificationController(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @GetMapping("/unread-count")
    public Result getUnreadCount(HttpServletRequest request) {
        return notificationQueryService.getUnreadCount(request);
    }

    @GetMapping("/list")
    public Result getNotificationList(
            @RequestParam(required = false) Integer notifyType,
            @RequestParam(required = false) Integer isRead,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        return notificationQueryService.getNotificationList(notifyType, isRead, page, size, request);
    }

    @GetMapping("/stats")
    public Result getNotificationStats(HttpServletRequest request) {
        return notificationQueryService.getNotificationStats(request);
    }

    @PutMapping("/read/{notifyId}")
    public Result markAsRead(@PathVariable Long notifyId, HttpServletRequest request) {
        return notificationQueryService.markAsRead(notifyId, request);
    }

    @PutMapping("/read-all")
    public Result markAllAsRead(HttpServletRequest request) {
        return notificationQueryService.markAllAsRead(request);
    }

    @DeleteMapping("/{notifyId}")
    public Result deleteNotification(@PathVariable Long notifyId, HttpServletRequest request) {
        return notificationQueryService.deleteNotification(notifyId, request);
    }

    @DeleteMapping("/clear-read")
    public Result clearReadNotifications(HttpServletRequest request) {
        return notificationQueryService.clearReadNotifications(request);
    }
}
