package com.example.demo.entity.vo;

import lombok.Data;

/**
 * 用户视图对象 - 用于返回给前端的数据封装
 * 只包含前端需要的字段，避免暴露敏感信息
 */
@Data
public class UserVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号（可脱敏处理）
     */
    private String phone;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 用户类型：1-租客 2-房东 3-管理员
     */
    private Integer userType;

    /**
     * 实名认证状态：0-未认证 1-认证中 2-已认证
     */
    private Integer realnameStatus;
}
