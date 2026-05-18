package com.example.demo.repository.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("rental_contract")
public class RentalContract {
    @TableId(type = IdType.AUTO)
    private Long contractId;
    
    private Long houseId;              // 房源ID
    private Long landlordId;           // 房东ID
    private Long tenantId;             // 租客ID
    private String contractNo;         // 合同编号
    private String content;            // 合同内容/合同文件路径
    private LocalDate rentStartDate;   // 租期开始日期
    private LocalDate rentEndDate;     // 租期结束日期
    private BigDecimal monthlyRent;    // 月租金
    private BigDecimal depositAmount;  // 押金金额
    private Integer paymentDay;        // 每月付款日
    private Integer status;            // 0草稿 1待确认 2已确认 3已到期 4已终止
    private Integer renewalStatus;     // 续租状态：0无/null 1申请中 2已同意 3已拒绝
    private Long parentContractId;     // 续租关联的原合同ID
    private LocalDateTime tenantSignTime;   // 租客确认时间
    private LocalDateTime landlordSignTime; // 房东签署时间（线下签署，平台不使用）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
