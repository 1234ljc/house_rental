package com.example.demo.repository.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.repository.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    // 根据用户名和用户类型查询（用于登录）
    default User selectByUsernameAndType(String username, Integer userType) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username)
                .eq(User::getUserType, userType);
        return selectOne(wrapper);
    }

    // 根据手机号查询
    default User selectByPhone(String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        return selectOne(wrapper);
    }

    // 根据邮箱查询
    default User selectByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return selectOne(wrapper);
    }

    /**
     * 按用户类型统计数量
     */
    @Select("SELECT user_type as userType, COUNT(*) as count FROM user GROUP BY user_type")
    List<Map<String, Object>> countByUserType();

    /**
     * 统计今日新增用户
     */
    @Select("SELECT COUNT(*) FROM user WHERE DATE(create_time) = CURDATE()")
    Long countTodayNew();

    /**
     * 近7天用户增长趋势
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as count FROM user " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> getLast7DaysTrend();

    /**
     * 近30天用户注册趋势（一条SQL）
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as count FROM user " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> getLast30DaysTrend();

    /**
     * 统计待审核实名认证用户数量
     */
    @Select("SELECT COUNT(*) FROM user WHERE realname_status = 3")
    Long countPendingRealname();
}