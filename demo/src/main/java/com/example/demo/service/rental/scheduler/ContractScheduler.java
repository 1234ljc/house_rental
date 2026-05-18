package com.example.demo.service.rental.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.rental.entity.RentalContract;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.repository.rental.RentalContractMapper;
import com.example.demo.service.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.repository.rental.entity.PaymentOrder;
import com.example.demo.repository.rental.PaymentOrderMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 合同定时任务
 * - 每天凌晨1点：将已到期合同更新为已到期，并恢复房源状态
 * - 每天凌晨2点：为活跃合同自动生成下月租金账单
 * - 每天上午9点：提前7天提醒即将到期的合同
 * - 每天上午10点：租金到期分级提醒（3天前、1天前、当天、逾期）
 */
@Slf4j
@Component
public class ContractScheduler {

    @Autowired
    private RentalContractMapper rentalContractMapper;

    @Autowired
    private HouseMapper houseMapper;

    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    @Autowired
    private NotificationService notificationService;

    /**
     * 每天凌晨1点执行：自动将已到期合同标记为"已到期"，并恢复房源为"可出租"
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void markExpiredContracts() {
        LocalDate today = LocalDate.now();

        // 查询所有已确认且租期已结束的合同
        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getStatus, 2)
               .lt(RentalContract::getRentEndDate, today);

        List<RentalContract> expiredContracts = rentalContractMapper.selectList(wrapper);

        for (RentalContract contract : expiredContracts) {
            // 更新合同状态为已到期
            LambdaUpdateWrapper<RentalContract> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(RentalContract::getContractId, contract.getContractId())
                         .set(RentalContract::getStatus, 3);
            rentalContractMapper.update(null, updateWrapper);

            // 恢复房源状态为可出租（status=1）
            House house = houseMapper.selectById(contract.getHouseId());
            if (house != null && house.getStatus() == 2) {
                LambdaUpdateWrapper<House> houseUpdate = new LambdaUpdateWrapper<>();
                houseUpdate.eq(House::getHouseId, house.getHouseId())
                           .set(House::getStatus, 1);
                houseMapper.update(null, houseUpdate);
            }
        }

        if (!expiredContracts.isEmpty()) {
            log.info("已处理 {} 份到期合同", expiredContracts.size());
        }
    }

    /**
     * 每天上午9点执行：提前7天提醒即将到期的合同
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void remindExpiringContracts() {
        LocalDate alertDate = LocalDate.now().plusDays(7);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getStatus, 2)
               .eq(RentalContract::getRentEndDate, alertDate);

        List<RentalContract> contracts = rentalContractMapper.selectList(wrapper);

        for (RentalContract contract : contracts) {
            House house = houseMapper.selectById(contract.getHouseId());
            String houseTitle = house != null ? house.getTitle() : "房源";
            String expireDate = contract.getRentEndDate().format(fmt);

            notificationService.notifyContractExpiring(
                contract.getTenantId(),
                contract.getLandlordId(),
                houseTitle,
                expireDate,
                contract.getContractId()
            );
        }

        if (!contracts.isEmpty()) {
            log.info("已发送 {} 份到期提醒", contracts.size());
        }
    }

    /**
     * 每天凌晨2点执行：为活跃合同自动生成下月租金账单
     * 在每月付款日前5天生成账单，避免重复生成
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateMonthlyBills() {
        LocalDate today = LocalDate.now();

        // 查询所有活跃合同（status=2）
        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getStatus, 2)
               .ge(RentalContract::getRentEndDate, today);

        List<RentalContract> activeContracts = rentalContractMapper.selectList(wrapper);
        int generated = 0;

        for (RentalContract contract : activeContracts) {
            Integer paymentDay = contract.getPaymentDay();
            if (paymentDay == null) paymentDay = 1;

            // 计算下一个付款日
            LocalDate nextPayDate;
            if (today.getDayOfMonth() < paymentDay) {
                // 本月付款日还没到
                nextPayDate = today.withDayOfMonth(Math.min(paymentDay, today.lengthOfMonth()));
            } else {
                // 本月付款日已过，看下个月
                LocalDate nextMonth = today.plusMonths(1);
                nextPayDate = nextMonth.withDayOfMonth(Math.min(paymentDay, nextMonth.lengthOfMonth()));
            }

            // 如果下一个付款日超过合同结束日，跳过
            if (nextPayDate.isAfter(contract.getRentEndDate())) continue;

            // 只在付款日前5天内生成
            long daysUntilPay = java.time.temporal.ChronoUnit.DAYS.between(today, nextPayDate);
            if (daysUntilPay > 5 || daysUntilPay < 0) continue;

            // 检查是否已存在该月的账单（避免重复）
            LambdaQueryWrapper<PaymentOrder> existCheck = new LambdaQueryWrapper<>();
            existCheck.eq(PaymentOrder::getContractId, contract.getContractId())
                      .eq(PaymentOrder::getOrderType, 1) // 租金支付
                      .ge(PaymentOrder::getCreateTime, nextPayDate.withDayOfMonth(1).atStartOfDay())
                      .le(PaymentOrder::getCreateTime, nextPayDate.withDayOfMonth(nextPayDate.lengthOfMonth()).atTime(23, 59, 59));

            if (paymentOrderMapper.selectCount(existCheck) > 0) continue;

            // 生成租金账单
            PaymentOrder order = new PaymentOrder();
            order.setContractId(contract.getContractId());
            order.setOrderNo("RENT" + System.currentTimeMillis() + (int)(Math.random() * 1000));
            order.setOrderType(1); // 租金支付
            order.setTotalAmount(contract.getMonthlyRent());
            order.setPayAmount(contract.getMonthlyRent());
            order.setPaymentStatus(0); // 待支付
            order.setCreateTime(LocalDateTime.now());
            paymentOrderMapper.insert(order);

            // 通知租客
            House house = houseMapper.selectById(contract.getHouseId());
            String houseTitle = house != null ? house.getTitle() : "房源";
            notificationService.notifyPaymentCreated(
                contract.getTenantId(),
                houseTitle,
                contract.getMonthlyRent().toString(),
                order.getOrderId()
            );

            generated++;
        }

        if (generated > 0) {
            log.info("已自动生成 {} 笔租金账单", generated);
        }
    }

    /**
     * 每天上午10点执行：租金到期分级提醒
     * - 到期前3天：温和提醒
     * - 到期前1天：紧急提醒
     * - 到期当天：最后提醒
     * - 逾期：逾期通知（通知房东+租客）
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void rentPaymentReminder() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 查询所有待支付的租金订单
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrder::getPaymentStatus, 0)
               .eq(PaymentOrder::getOrderType, 1); // 租金支付

        List<PaymentOrder> pendingOrders = paymentOrderMapper.selectList(wrapper);
        int reminded = 0;

        for (PaymentOrder order : pendingOrders) {
            RentalContract contract = rentalContractMapper.selectById(order.getContractId());
            if (contract == null || contract.getStatus() != 2) continue;

            House house = houseMapper.selectById(contract.getHouseId());
            String houseTitle = house != null ? house.getTitle() : "房源";

            // 计算账单创建后的天数（作为到期参考）
            LocalDate createDate = order.getCreateTime().toLocalDate();
            long daysSinceCreate = java.time.temporal.ChronoUnit.DAYS.between(createDate, today);

            if (daysSinceCreate == 4) {
                // 创建后第4天（假设5天内需支付），提前1天提醒
                notificationService.send(contract.getTenantId(),
                    NotificationService.TYPE_PAYMENT,
                    "租金明天到期",
                    "【" + houseTitle + "】的租金 ¥" + order.getPayAmount() + " 明天到期，请及时缴纳",
                    order.getOrderId());
                reminded++;
            } else if (daysSinceCreate == 2) {
                // 创建后第2天，提前3天提醒
                notificationService.send(contract.getTenantId(),
                    NotificationService.TYPE_PAYMENT,
                    "租金缴纳提醒",
                    "【" + houseTitle + "】有一笔 ¥" + order.getPayAmount() + " 的租金待缴纳",
                    order.getOrderId());
                reminded++;
            } else if (daysSinceCreate >= 7) {
                // 逾期超过7天，通知房东
                // 每7天提醒一次房东
                if (daysSinceCreate % 7 == 0) {
                    notificationService.notifyPaymentOverdue(contract.getTenantId(), houseTitle, order.getOrderId());
                    notificationService.send(contract.getLandlordId(),
                        NotificationService.TYPE_PAYMENT,
                        "租金逾期提醒",
                        "【" + houseTitle + "】的租金已逾期 " + (daysSinceCreate - 5) + " 天，请关注",
                        order.getOrderId());
                    reminded++;
                }
            }
        }

        if (reminded > 0) {
            log.info("已发送 {} 条租金提醒", reminded);
        }
    }
}
