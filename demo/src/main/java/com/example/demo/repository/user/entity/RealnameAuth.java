package com.example.demo.repository.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("realname_auth")
public class RealnameAuth {
    @TableId(type = IdType.AUTO)
    private Long authId;
    
    private Long userId;
    private String realName;
    private String idCard;
    private String idCardFront;
    private String idCardBack;
    private String handheldPhoto;
    private Integer authStatus; // 0待审核 1已通过 2已驳回
    private String auditReason;
    private Long auditorId;
    private LocalDateTime auditTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
