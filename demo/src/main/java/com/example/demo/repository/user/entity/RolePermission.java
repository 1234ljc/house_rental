package com.example.demo.repository.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("role_permission")
public class RolePermission {
    @TableId(type = IdType.AUTO)
    private Long rolePermId;
    
    private Long roleId;
    private Long permId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
