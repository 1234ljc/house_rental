package com.example.demo.repository.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_role")
public class UserRole {
    @TableId(type = IdType.AUTO)
    private Long userRoleId;
    
    private Long userId;
    private Long roleId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
