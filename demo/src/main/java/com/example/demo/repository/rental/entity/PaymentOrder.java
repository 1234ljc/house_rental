package com.example.demo.repository.rental.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_order")
public class PaymentOrder {
    @TableId(type = IdType.AUTO)
    private Long orderId;
    
    private Long contractId;
    private String orderNo;
    private Integer orderType; // 0首期支付 1租金支付 2押金退还
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer paymentMethod; // 1支付宝 2微信 3银行卡
    private Integer paymentStatus; // 0待支付 1已支付 2支付失败
    private LocalDateTime paymentTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
