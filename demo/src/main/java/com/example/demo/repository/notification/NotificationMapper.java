package com.example.demo.repository.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.repository.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 统计未读消息数
     */
    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    Long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 按通知类型分组统计未读数量（一条SQL替代多次查询）
     */
    @Select("SELECT notify_type as notifyType, COUNT(*) as cnt FROM notification WHERE user_id = #{userId} AND is_read = 0 GROUP BY notify_type")
    List<Map<String, Object>> countUnreadByType(@Param("userId") Long userId);

    /**
     * 标记所有消息为已读
     */
    @Update("UPDATE notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllAsRead(@Param("userId") Long userId);
}
