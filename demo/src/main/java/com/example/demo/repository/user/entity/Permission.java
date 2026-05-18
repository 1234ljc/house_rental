package com.example.demo.repository.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("permission")
public class Permission {
    @TableId(type = IdType.AUTO)
    private Long permId;
    
    private String permCode;
    private String permName;
    private Integer permType; // 1菜单 2按钮 3接口
    private Long parentId;
    private String path;
    private String component;
    private String icon;
    private Integer orderNum;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    // 子菜单（非数据库字段）
    @TableField(exist = false)
    private List<Permission> children;
}
