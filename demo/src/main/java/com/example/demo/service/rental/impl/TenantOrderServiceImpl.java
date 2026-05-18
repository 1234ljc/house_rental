package com.example.demo.service.rental.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Result;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.repository.rental.entity.PaymentOrder;
import com.example.demo.repository.rental.entity.RentalContract;
import com.example.demo.repository.rental.PaymentOrderMapper;
import com.example.demo.repository.rental.RentalContractMapper;
import com.example.demo.service.rental.TenantOrderService;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.UserMapper;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TenantOrderServiceImpl implements TenantOrderService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final RentalContractMapper rentalContractMapper;
    private final HouseMapper houseMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public TenantOrderServiceImpl(PaymentOrderMapper paymentOrderMapper,
                                  RentalContractMapper rentalContractMapper,
                                  HouseMapper houseMapper,
                                  UserMapper userMapper,
                                  NotificationService notificationService) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.rentalContractMapper = rentalContractMapper;
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Override
    public Result getOrderStats(HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        Map<String, Object> stats = new HashMap<>();
        List<Long> contractIds = getContractIdsByTenant(tenantId);
        if (contractIds.isEmpty()) {
            stats.put("pending", 0);
            stats.put("paid", 0);
            stats.put("totalPaid", BigDecimal.ZERO);
            stats.put("depositRefunding", 0);
        } else {
            LambdaQueryWrapper<PaymentOrder> pendingWrapper = new LambdaQueryWrapper<>();
            pendingWrapper.in(PaymentOrder::getContractId, contractIds).eq(PaymentOrder::getPaymentStatus, 0);
            stats.put("pending", paymentOrderMapper.selectCount(pendingWrapper));

            LambdaQueryWrapper<PaymentOrder> paidWrapper = new LambdaQueryWrapper<>();
            paidWrapper.in(PaymentOrder::getContractId, contractIds).eq(PaymentOrder::getPaymentStatus, 1);
            stats.put("paid", paymentOrderMapper.selectCount(paidWrapper));

            LambdaQueryWrapper<PaymentOrder> totalWrapper = new LambdaQueryWrapper<>();
            totalWrapper.in(PaymentOrder::getContractId, contractIds).eq(PaymentOrder::getPaymentStatus, 1).ne(PaymentOrder::getOrderType, 2);
            List<PaymentOrder> paidOrders = paymentOrderMapper.selectList(totalWrapper);
            BigDecimal totalPaid = paidOrders.stream().map(PaymentOrder::getPayAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.put("totalPaid", totalPaid);

            LambdaQueryWrapper<PaymentOrder> refundWrapper = new LambdaQueryWrapper<>();
            refundWrapper.in(PaymentOrder::getContractId, contractIds).eq(PaymentOrder::getOrderType, 2).eq(PaymentOrder::getPaymentStatus, 0);
            stats.put("depositRefunding", paymentOrderMapper.selectCount(refundWrapper));
        }
        User user = userMapper.selectById(tenantId);
        stats.put("beans", user != null && user.getBeans() != null ? user.getBeans() : 0);
        return Result.success(stats);
    }

    @Override
    public Result getBeansInfo(HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        User user = userMapper.selectById(tenantId);
        Map<String, Object> info = new HashMap<>();
        int beans = user != null && user.getBeans() != null ? user.getBeans() : 0;
        info.put("beans", beans);
        info.put("beansValue", new BigDecimal(beans).divide(new BigDecimal(1000), 2, RoundingMode.DOWN));

        List<Long> contractIds = getContractIdsByTenant(tenantId);
        if (!contractIds.isEmpty()) {
            LambdaQueryWrapper<PaymentOrder> paidWrapper = new LambdaQueryWrapper<>();
            paidWrapper.in(PaymentOrder::getContractId, contractIds).eq(PaymentOrder::getPaymentStatus, 1).ne(PaymentOrder::getOrderType, 2);
            List<PaymentOrder> paidOrders = paymentOrderMapper.selectList(paidWrapper);
            int totalEarned = paidOrders.stream().mapToInt(o -> o.getPayAmount().multiply(new BigDecimal(10)).intValue()).sum();
            info.put("totalEarned", totalEarned);
            info.put("totalUsed", Math.max(0, totalEarned - beans));
        } else {
            info.put("totalEarned", 0);
            info.put("totalUsed", 0);
        }
        return Result.success(info);
    }

    @Override
    public Result calcBeansForOrder(Long orderId, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        PaymentOrder order = paymentOrderMapper.selectById(orderId);
        if (order == null) return Result.failure("订单不存在");
        RentalContract contract = rentalContractMapper.selectById(order.getContractId());
        if (contract == null || !contract.getTenantId().equals(tenantId)) return Result.failure("无权查看此订单");
        User user = userMapper.selectById(tenantId);
        int currentBeans = user != null && user.getBeans() != null ? user.getBeans() : 0;
        BigDecimal maxDiscount = order.getTotalAmount().multiply(new BigDecimal("0.1"));
        int maxUsableBeans = maxDiscount.multiply(new BigDecimal(1000)).intValue();
        int usableBeans = Math.min(currentBeans, maxUsableBeans);
        BigDecimal discountAmount = new BigDecimal(usableBeans).divide(new BigDecimal(1000), 2, RoundingMode.DOWN);
        Map<String, Object> result = new HashMap<>();
        result.put("currentBeans", currentBeans);
        result.put("maxUsableBeans", maxUsableBeans);
        result.put("usableBeans", usableBeans);
        result.put("discountAmount", discountAmount);
        result.put("orderAmount", order.getTotalAmount());
        result.put("actualPay", order.getTotalAmount().subtract(discountAmount));
        return Result.success(result);
    }

    @Override
    public Result getOrderList(Integer orderType, Integer paymentStatus, Integer page, Integer size, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        List<Long> contractIds = getContractIdsByTenant(tenantId);
        if (contractIds.isEmpty()) return Result.success(Map.of("records", new ArrayList<>(), "total", 0));
        Page<PaymentOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PaymentOrder::getContractId, contractIds);
        if (orderType != null) wrapper.eq(PaymentOrder::getOrderType, orderType);
        if (paymentStatus != null) wrapper.eq(PaymentOrder::getPaymentStatus, paymentStatus);
        wrapper.orderByDesc(PaymentOrder::getCreateTime);
        Page<PaymentOrder> result = paymentOrderMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (PaymentOrder order : result.getRecords()) records.add(buildOrderInfo(order));
        return Result.success(Map.of("records", records, "total", result.getTotal()));
    }

    @Override
    public Result getOrderDetail(Long orderId, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        PaymentOrder order = paymentOrderMapper.selectById(orderId);
        if (order == null) return Result.failure("订单不存在");
        RentalContract contract = rentalContractMapper.selectById(order.getContractId());
        if (contract == null || !contract.getTenantId().equals(tenantId)) return Result.failure("无权查看此订单");
        return Result.success(buildOrderInfo(order));
    }

    @Override
    public Result payOrder(Long orderId, Map<String, Object> params, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        PaymentOrder order = paymentOrderMapper.selectById(orderId);
        if (order == null) return Result.failure("订单不存在");
        RentalContract contract = rentalContractMapper.selectById(order.getContractId());
        if (contract == null || !contract.getTenantId().equals(tenantId)) return Result.failure("无权操作此订单");
        if (order.getPaymentStatus() != 0) return Result.failure("该订单已支付或已失败");
        Integer paymentMethod = params.get("paymentMethod") != null ? Integer.valueOf(params.get("paymentMethod").toString()) : 1;
        Integer useBeans = params.get("useBeans") != null ? Integer.valueOf(params.get("useBeans").toString()) : 0;
        User tenant = userMapper.selectById(tenantId);
        if (tenant == null) return Result.failure("用户不存在");
        int currentBeans = tenant.getBeans() != null ? tenant.getBeans() : 0;
        if (useBeans > currentBeans) return Result.failure("支付豆余额不足");
        BigDecimal beansDiscount = new BigDecimal(useBeans).divide(new BigDecimal(1000), 2, RoundingMode.DOWN);
        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal maxDiscount = totalAmount.multiply(new BigDecimal("0.1"));
        if (beansDiscount.compareTo(maxDiscount) > 0) {
            beansDiscount = maxDiscount;
            useBeans = maxDiscount.multiply(new BigDecimal(1000)).intValue();
        }
        BigDecimal actualPay = totalAmount.subtract(beansDiscount);
        if (actualPay.compareTo(BigDecimal.ZERO) < 0) actualPay = BigDecimal.ZERO;
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(1);
        order.setPaymentTime(LocalDateTime.now());
        order.setPayAmount(actualPay);
        paymentOrderMapper.updateById(order);
        if (useBeans > 0) {
            tenant.setBeans(currentBeans - useBeans);
            userMapper.updateById(tenant);
        }
        int earnBeans = actualPay.multiply(new BigDecimal(10)).intValue();
        if (earnBeans > 0) {
            tenant.setBeans(tenant.getBeans() + earnBeans);
            userMapper.updateById(tenant);
        }
        String tenantName = tenant.getRealName() != null ? tenant.getRealName() : "租客";
        House house = houseMapper.selectById(contract.getHouseId());
        notificationService.notifyPaymentSuccess(contract.getLandlordId(), tenantName, house != null ? house.getTitle() : "房源", actualPay.toString(), orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("actualPay", actualPay);
        result.put("beansUsed", useBeans);
        result.put("beansDiscount", beansDiscount);
        result.put("beansEarned", earnBeans);
        result.put("currentBeans", tenant.getBeans());
        return Result.success(result);
    }

    @Override
    public Result applyDepositRefund(Long contractId, Map<String, String> params, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null || !contract.getTenantId().equals(tenantId)) return Result.failure("合同不存在或无权操作");
        if (contract.getStatus() != 3 && contract.getStatus() != 4) return Result.failure("只有已到期或已终止的合同才能申请押金退还");
        LambdaQueryWrapper<PaymentOrder> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(PaymentOrder::getContractId, contractId).eq(PaymentOrder::getOrderType, 2);
        if (paymentOrderMapper.selectCount(checkWrapper) > 0) return Result.failure("已有押金退还申请");
        PaymentOrder refundOrder = new PaymentOrder();
        refundOrder.setContractId(contractId);
        refundOrder.setOrderNo(generateOrderNo("DR"));
        refundOrder.setOrderType(2);
        refundOrder.setTotalAmount(contract.getDepositAmount());
        refundOrder.setPayAmount(BigDecimal.ZERO);
        refundOrder.setPaymentStatus(0);
        refundOrder.setCreateTime(LocalDateTime.now());
        paymentOrderMapper.insert(refundOrder);
        User tenant = userMapper.selectById(tenantId);
        String tenantName = tenant != null && tenant.getRealName() != null ? tenant.getRealName() : "租客";
        House house = houseMapper.selectById(contract.getHouseId());
        notificationService.notifyDepositRefundApplied(contract.getLandlordId(), tenantName, house != null ? house.getTitle() : "房源", refundOrder.getOrderId());
        return Result.success("押金退还申请已提交，请等待房东处理");
    }

    @Override
    public Result getPendingOrders(HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        List<Long> contractIds = getContractIdsByTenant(tenantId);
        if (contractIds.isEmpty()) return Result.success(new ArrayList<>());
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PaymentOrder::getContractId, contractIds).eq(PaymentOrder::getPaymentStatus, 0).ne(PaymentOrder::getOrderType, 2).orderByAsc(PaymentOrder::getCreateTime);
        List<PaymentOrder> orders = paymentOrderMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (PaymentOrder order : orders) result.add(buildOrderInfo(order));
        return Result.success(result);
    }

    @Override
    public Result initOrdersForExistingContracts(HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getTenantId, tenantId).eq(RentalContract::getStatus, 2);
        List<RentalContract> contracts = rentalContractMapper.selectList(wrapper);
        int created = 0;
        for (RentalContract contract : contracts) {
            LambdaQueryWrapper<PaymentOrder> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(PaymentOrder::getContractId, contract.getContractId()).eq(PaymentOrder::getOrderType, 0);
            if (paymentOrderMapper.selectCount(checkWrapper) == 0) {
                PaymentOrder order = new PaymentOrder();
                order.setContractId(contract.getContractId());
                order.setOrderNo(generateOrderNo("FP"));
                order.setOrderType(0);
                order.setTotalAmount(contract.getMonthlyRent().add(contract.getDepositAmount()));
                order.setPayAmount(BigDecimal.ZERO);
                order.setPaymentStatus(0);
                order.setCreateTime(LocalDateTime.now());
                paymentOrderMapper.insert(order);
                created++;
            }
        }
        return Result.success("已为 " + created + " 个合同生成首期支付订单");
    }

    private List<Long> getContractIdsByTenant(Long tenantId) {
        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getTenantId, tenantId).select(RentalContract::getContractId);
        List<RentalContract> contracts = rentalContractMapper.selectList(wrapper);
        return contracts.stream().map(RentalContract::getContractId).toList();
    }

    private Map<String, Object> buildOrderInfo(PaymentOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", order.getOrderId());
        map.put("orderNo", order.getOrderNo());
        map.put("orderType", order.getOrderType());
        map.put("totalAmount", order.getTotalAmount());
        map.put("payAmount", order.getPayAmount());
        map.put("paymentMethod", order.getPaymentMethod());
        map.put("paymentStatus", order.getPaymentStatus());
        map.put("paymentTime", order.getPaymentTime());
        map.put("createTime", order.getCreateTime());
        RentalContract contract = rentalContractMapper.selectById(order.getContractId());
        if (contract != null) {
            Map<String, Object> contractInfo = new HashMap<>();
            contractInfo.put("contractId", contract.getContractId());
            contractInfo.put("contractNo", contract.getContractNo());
            contractInfo.put("monthlyRent", contract.getMonthlyRent());
            contractInfo.put("depositAmount", contract.getDepositAmount());
            map.put("contract", contractInfo);
            House house = houseMapper.selectById(contract.getHouseId());
            if (house != null) {
                Map<String, Object> houseInfo = new HashMap<>();
                houseInfo.put("houseId", house.getHouseId());
                houseInfo.put("title", house.getTitle());
                houseInfo.put("address", house.getAddress());
                houseInfo.put("images", house.getImages());
                map.put("house", houseInfo);
            }
            User landlord = userMapper.selectById(contract.getLandlordId());
            if (landlord != null) {
                Map<String, Object> landlordInfo = new HashMap<>();
                landlordInfo.put("userId", landlord.getUserId());
                landlordInfo.put("realName", landlord.getRealName());
                landlordInfo.put("phone", landlord.getPhone());
                map.put("landlord", landlordInfo);
            }
        }
        return map;
    }

    private String generateOrderNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + String.format("%04d", new Random().nextInt(10000));
    }
}
