package com.example.demo.repository.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rental_manage")
public class RentalManage {
    @TableId(type = IdType.AUTO)
    private Long manageId;
    
    private Long contractId;       // 合同ID
    private Long userId;           // 操作人ID
    private Integer manageType;    // 类型：0维修申请 1其他问题
    private String content;        // 内容/问题描述
    private String images;         // 图片(JSON)
    private Integer status;        // 状态：0待处理 1处理中 2已完成
    private String responseContent; // 处理反馈
    private Integer rating;        // 评分(1-5)
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    private LocalDateTime completeTime; // 完成时间
}
