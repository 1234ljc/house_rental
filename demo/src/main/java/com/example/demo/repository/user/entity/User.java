package com.example.demo.repository.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("`user`") // 使用反引号避免SQL关键字冲突
public class User {
    @TableId(type = IdType.AUTO)
    private Long userId;

    private String username;
    private String password;
    private String phone;
    private String email;
    private String realName;
    private String idCard;
    private String avatar;

    private Integer userType; // 1租客 2房东 3管理员
    private Integer realnameStatus;
    private LocalDateTime realnameTime;
    private String realnameAuditReason;
    private Integer status;
    
    private Integer beans; // 支付豆余额，1000支付豆=1元
    
    private Integer creditScore; // 信用评分(0-1000)

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}