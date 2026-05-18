package com.example.demo.repository.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("role")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long roleId;
    
    private String roleCode;
    private String roleName;
    private String roleDesc;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
