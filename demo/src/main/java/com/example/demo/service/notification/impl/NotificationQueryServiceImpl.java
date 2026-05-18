package com.example.demo.service.notification.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Result;
import com.example.demo.repository.notification.entity.Notification;
import com.example.demo.repository.notification.NotificationMapper;
import com.example.demo.service.notification.NotificationQueryService;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationMapper notificationMapper;

    public NotificationQueryServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public Result getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long count = notificationMapper.countUnreadByUserId(userId);
        return Result.success(count != null ? count : 0);
    }

    @Override
    public Result getNotificationList(Integer notifyType, Integer isRead, Integer page, Integer size, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        Page<Notification> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);

        if (notifyType != null) {
            wrapper.eq(Notification::getNotifyType, notifyType);
        }
        if (isRead != null) {
            wrapper.eq(Notification::getIsRead, isRead);
        }
        wrapper.orderByDesc(Notification::getCreateTime);

        Page<Notification> result = notificationMapper.selectPage(pageParam, wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Notification notify : result.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("notifyId", notify.getNotifyId());
            map.put("notifyType", notify.getNotifyType());
            map.put("title", notify.getTitle());
            map.put("content", notify.getContent());
            map.put("relatedId", notify.getRelatedId());
            map.put("isRead", notify.getIsRead());
            map.put("createTime", notify.getCreateTime());
            records.add(map);
        }

        return Result.success(Map.of("records", records, "total", result.getTotal()));
    }

    @Override
    public Result getNotificationStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        Map<String, Object> stats = new HashMap<>();
        Long unread = notificationMapper.countUnreadByUserId(userId);
        stats.put("unread", unread != null ? unread : 0);

        List<Map<String, Object>> rows = notificationMapper.countUnreadByType(userId);
        Map<Integer, Long> typeMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Integer type = ((Number) row.get("notifyType")).intValue();
            Long cnt = ((Number) row.get("cnt")).longValue();
            typeMap.put(type, cnt);
        }
        for (int type = 1; type <= 6; type++) {
            stats.put("type" + type, typeMap.getOrDefault(type, 0L));
        }

        return Result.success(stats);
    }

    @Override
    public Result markAsRead(Long notifyId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Notification notify = notificationMapper.selectById(notifyId);
        if (notify == null || !notify.getUserId().equals(userId)) {
            return Result.failure("消息不存在");
        }
        notify.setIsRead(1);
        notificationMapper.updateById(notify);
        return Result.success("已标记为已读");
    }

    @Override
    public Result markAllAsRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        notificationMapper.markAllAsRead(userId);
        return Result.success("已全部标记为已读");
    }

    @Override
    public Result deleteNotification(Long notifyId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Notification notify = notificationMapper.selectById(notifyId);
        if (notify == null || !notify.getUserId().equals(userId)) {
            return Result.failure("消息不存在");
        }
        notificationMapper.deleteById(notifyId);
        return Result.success("已删除");
    }

    @Override
    public Result clearReadNotifications(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId).eq(Notification::getIsRead, 1);
        notificationMapper.delete(wrapper);
        return Result.success("已清除已读消息");
    }
}
