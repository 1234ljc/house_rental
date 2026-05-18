package com.example.demo.entity.vo;

import lombok.Data;

/**
 * 房东视图对象 - 用于返回给前端的数据封装
 */
@Data
public class LandlordVO {

    /**
     * 房东ID
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
     * 手机号（可脱敏）
     */
    private String phone;

    /**
     * 头像
     */
    private String avatar;
}
