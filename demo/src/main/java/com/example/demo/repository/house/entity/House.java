package com.example.demo.repository.house.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("house")
public class House {
    @TableId(type = IdType.AUTO)
    private Long houseId;
    
    private Long landlordId;
    private String title;
    private String description;
    private String address;
    private String province;
    private String city;
    private String district;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private BigDecimal rentPrice;
    private String depositType; // 押付方式：押一付一、押一付二、押一付三等
    private BigDecimal area;
    private String houseType;
    private String floor;
    private String orientation;
    private String facilities; // JSON
    private String images; // JSON
    private Integer rentOption; // 1整租 2合租 3都支持
    private Integer status; // 0待审核 1可出租 2已出租 3已下架 4审核驳回
    private String auditReason;
    private Integer viewCount;
    private Integer collectCount;
    private String propertyLicenseFront;
    private String propertyLicenseBack;
    private String propertyLicenseOther;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
