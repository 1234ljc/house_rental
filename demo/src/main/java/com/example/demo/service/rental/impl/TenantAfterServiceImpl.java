package com.example.demo.service.rental.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Result;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.repository.rental.entity.PaymentOrder;
import com.example.demo.repository.rental.entity.RentalContract;
import com.example.demo.repository.rental.entity.RentalManage;
import com.example.demo.repository.rental.PaymentOrderMapper;
import com.example.demo.repository.rental.RentalContractMapper;
import com.example.demo.repository.rental.RentalManageMapper;
import com.example.demo.service.rental.TenantAfterService;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.UserMapper;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TenantAfterServiceImpl implements TenantAfterService {

    private final RentalManageMapper rentalManageMapper;
    private final RentalContractMapper rentalContractMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final HouseMapper houseMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public TenantAfterServiceImpl(RentalManageMapper rentalManageMapper,
                                  RentalContractMapper rentalContractMapper,
                                  PaymentOrderMapper paymentOrderMapper,
                                  HouseMapper houseMapper,
                                  UserMapper userMapper,
                                  NotificationService notificationService) {
        this.rentalManageMapper = rentalManageMapper;
        this.rentalContractMapper = rentalContractMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Override
    public Result getRentList(Long contractId, Integer page, Integer size, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        List<Long> contractIds;
        if (contractId != null) {
            contractIds = List.of(contractId);
        } else {
            LambdaQueryWrapper<RentalContract> cw = new LambdaQueryWrapper<>();
            cw.eq(RentalContract::getTenantId, tenantId).eq(RentalContract::getStatus, 2).select(RentalContract::getContractId);
            List<RentalContract> contracts = rentalContractMapper.selectList(cw);
            contractIds = contracts.stream().map(RentalContract::getContractId).toList();
        }
        if (CollUtil.isEmpty(contractIds)) return Result.success(Map.of("records", new ArrayList<>(), "total", 0));

        Page<PaymentOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PaymentOrder::getContractId, contractIds).in(PaymentOrder::getOrderType, Arrays.asList(0, 1)).orderByDesc(PaymentOrder::getCreateTime);
        Page<PaymentOrder> result = paymentOrderMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (PaymentOrder order : result.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("orderId", order.getOrderId());
            map.put("orderNo", order.getOrderNo());
            map.put("orderType", order.getOrderType());
            map.put("totalAmount", order.getTotalAmount());
            map.put("payAmount", order.getPayAmount());
            map.put("paymentStatus", order.getPaymentStatus());
            map.put("paymentTime", order.getPaymentTime());
            map.put("createTime", order.getCreateTime());
            map.put("isOverdue", order.getPaymentStatus() == 0 && order.getCreateTime().isBefore(LocalDateTime.now().minusDays(3)));
            RentalContract contract = rentalContractMapper.selectById(order.getContractId());
            if (contract != null) {
                map.put("contractNo", contract.getContractNo());
                map.put("paymentDay", contract.getPaymentDay());
                House house = houseMapper.selectById(contract.getHouseId());
                if (house != null) {
                    map.put("houseTitle", house.getTitle());
                    map.put("houseAddress", house.getAddress());
                }
            }
            records.add(map);
        }
        return Result.success(Map.of("records", records, "total", result.getTotal()));
    }

    @Override
    public Result getActiveContracts(HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getTenantId, tenantId).in(RentalContract::getStatus, Arrays.asList(2, 3)).orderByDesc(RentalContract::getCreateTime);
        List<RentalContract> contracts = rentalContractMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (RentalContract contract : contracts) {
            Map<String, Object> map = new HashMap<>();
            map.put("contractId", contract.getContractId());
            map.put("contractNo", contract.getContractNo());
            map.put("status", contract.getStatus());
            map.put("rentStartDate", contract.getRentStartDate());
            map.put("rentEndDate", contract.getRentEndDate());
            map.put("monthlyRent", contract.getMonthlyRent());
            map.put("paymentDay", contract.getPaymentDay());
            House house = houseMapper.selectById(contract.getHouseId());
            if (house != null) {
                map.put("houseId", house.getHouseId());
                map.put("houseTitle", house.getTitle());
                map.put("houseAddress", house.getAddress());
                map.put("houseImages", house.getImages());
            }
            User landlord = userMapper.selectById(contract.getLandlordId());
            if (landlord != null) {
                map.put("landlordName", landlord.getRealName());
                map.put("landlordPhone", landlord.getPhone());
            }
            result.add(map);
        }
        return Result.success(result);
    }

    @Override
    public Result submitIssue(Map<String, Object> params, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        Long contractId = Long.valueOf(params.get("contractId").toString());
        String content = (String) params.get("content");
        String images = params.get("images") != null ? params.get("images").toString() : null;
        Integer manageType = params.get("manageType") != null ? Integer.valueOf(params.get("manageType").toString()) : 0;
        if (StrUtil.isBlank(content)) return Result.failure("请填写问题描述");
        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null || !contract.getTenantId().equals(tenantId)) return Result.failure("合同不存在或无权操作");
        if (contract.getStatus() != 2) return Result.failure("只有生效中的合同才能提交问题反馈");

        RentalManage manage = new RentalManage();
        manage.setContractId(contractId);
        manage.setUserId(tenantId);
        manage.setManageType(manageType);
        manage.setContent(content);
        manage.setImages(images);
        manage.setStatus(0);
        manage.setCreateTime(LocalDateTime.now());
        rentalManageMapper.insert(manage);

        User tenant = userMapper.selectById(tenantId);
        String tenantName = tenant != null && tenant.getRealName() != null ? tenant.getRealName() : "租客";
        House house = houseMapper.selectById(contract.getHouseId());
        notificationService.notifyIssueCreated(contract.getLandlordId(), tenantName, house != null ? house.getTitle() : "房源", manage.getManageId());
        return Result.success("问题已提交，请等待房东处理");
    }

    @Override
    public Result getIssueList(Integer status, Integer page, Integer size, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        Page<RentalManage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RentalManage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalManage::getUserId, tenantId);
        if (status != null) wrapper.eq(RentalManage::getStatus, status);
        wrapper.orderByDesc(RentalManage::getCreateTime);
        Page<RentalManage> result = rentalManageMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (RentalManage manage : result.getRecords()) records.add(buildIssueInfo(manage));
        return Result.success(Map.of("records", records, "total", result.getTotal()));
    }

    @Override
    public Result appendIssue(Long manageId, Map<String, String> params, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        RentalManage manage = rentalManageMapper.selectById(manageId);
        if (manage == null || !manage.getUserId().equals(tenantId)) return Result.failure("记录不存在或无权操作");
        if (manage.getStatus() == 2) return Result.failure("问题已解决，无法补充");
        String appendContent = params.get("content");
        if (StrUtil.isNotBlank(appendContent)) {
            manage.setContent(manage.getContent() + "\n\n【补充】" + appendContent);
            rentalManageMapper.updateById(manage);
        }
        return Result.success("补充成功");
    }

    @Override
    public Result applyCheckout(Map<String, Object> params, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        Long contractId = Long.valueOf(params.get("contractId").toString());
        String expectDate = (String) params.get("expectDate");
        String reason = (String) params.get("reason");
        Integer checkoutType = params.get("checkoutType") != null ? Integer.valueOf(params.get("checkoutType").toString()) : 2;
        if (StrUtil.isBlank(reason)) return Result.failure("请填写退租原因");
        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null || !contract.getTenantId().equals(tenantId)) return Result.failure("合同不存在或无权操作");
        if (contract.getStatus() != 2) return Result.failure("只有生效中的合同才能申请退租");

        LambdaQueryWrapper<RentalManage> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(RentalManage::getContractId, contractId).eq(RentalManage::getManageType, 2).in(RentalManage::getStatus, Arrays.asList(0, 1, 3));
        if (rentalManageMapper.selectCount(checkWrapper) > 0) return Result.failure("您已有待处理的退租申请");

        String content = String.format("退租类型：%s\n期望退租日期：%s\n退租原因：%s", checkoutType == 1 ? "正常到期" : "提前退租", expectDate, reason);
        RentalManage manage = new RentalManage();
        manage.setContractId(contractId);
        manage.setUserId(tenantId);
        manage.setManageType(2);
        manage.setContent(content);
        manage.setStatus(0);
        manage.setCreateTime(LocalDateTime.now());
        rentalManageMapper.insert(manage);

        User tenant = userMapper.selectById(tenantId);
        String tenantName = tenant != null && tenant.getRealName() != null ? tenant.getRealName() : "租客";
        House house = houseMapper.selectById(contract.getHouseId());
        notificationService.notifyCheckoutApply(contract.getLandlordId(), tenantName, house != null ? house.getTitle() : "房源", manage.getManageId());
        return Result.success("退租申请已提交，请等待房东处理");
    }

    @Override
    public Result getCheckoutList(HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<RentalManage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalManage::getUserId, tenantId).eq(RentalManage::getManageType, 2).orderByDesc(RentalManage::getCreateTime);
        List<RentalManage> manages = rentalManageMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (RentalManage manage : manages) result.add(buildCheckoutInfo(manage));
        return Result.success(result);
    }

    @Override
    public Result getCheckoutDetail(Long manageId, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        RentalManage manage = rentalManageMapper.selectById(manageId);
        if (manage == null || !manage.getUserId().equals(tenantId) || manage.getManageType() != 2) return Result.failure("退租申请不存在或无权查看");
        return Result.success(buildCheckoutInfo(manage));
    }

    @Override
    public Result cancelCheckout(Long manageId, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        RentalManage manage = rentalManageMapper.selectById(manageId);
        if (manage == null || !manage.getUserId().equals(tenantId) || manage.getManageType() != 2) return Result.failure("退租申请不存在或无权操作");
        if (manage.getStatus() != 0) return Result.failure("只能取消待审核的退租申请");
        rentalManageMapper.deleteById(manageId);
        return Result.success("退租申请已取消");
    }

    @Override
    public Result confirmCheckout(Long manageId, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        RentalManage manage = rentalManageMapper.selectById(manageId);
        if (manage == null || !manage.getUserId().equals(tenantId) || manage.getManageType() != 2) return Result.failure("退租申请不存在或无权操作");
        if (manage.getStatus() != 3) return Result.failure("当前状态不能确认交接");
        manage.setStatus(4);
        manage.setCompleteTime(LocalDateTime.now());
        rentalManageMapper.updateById(manage);
        RentalContract contract = rentalContractMapper.selectById(manage.getContractId());
        if (contract != null) {
            contract.setStatus(4);
            contract.setUpdateTime(LocalDateTime.now());
            rentalContractMapper.updateById(contract);
            House house = houseMapper.selectById(contract.getHouseId());
            if (house != null) {
                house.setStatus(1);
                houseMapper.updateById(house);
            }
            notificationService.notifyCheckoutComplete(contract.getLandlordId(), house != null ? house.getTitle() : "房源", manage.getManageId());
        }
        return Result.success("交接确认完成，合同已终止");
    }

    private Map<String, Object> buildIssueInfo(RentalManage manage) {
        Map<String, Object> map = new HashMap<>();
        map.put("manageId", manage.getManageId());
        map.put("manageType", manage.getManageType());
        map.put("content", manage.getContent());
        map.put("images", manage.getImages());
        map.put("status", manage.getStatus());
        map.put("responseContent", manage.getResponseContent());
        map.put("createTime", manage.getCreateTime());
        map.put("completeTime", manage.getCompleteTime());
        RentalContract contract = rentalContractMapper.selectById(manage.getContractId());
        if (contract != null) {
            map.put("contractNo", contract.getContractNo());
            House house = houseMapper.selectById(contract.getHouseId());
            if (house != null) map.put("houseTitle", house.getTitle());
            User landlord = userMapper.selectById(contract.getLandlordId());
            if (landlord != null) map.put("landlordName", landlord.getRealName());
        }
        return map;
    }

    private Map<String, Object> buildCheckoutInfo(RentalManage manage) {
        Map<String, Object> map = new HashMap<>();
        map.put("manageId", manage.getManageId());
        map.put("content", manage.getContent());
        map.put("status", manage.getStatus());
        map.put("responseContent", manage.getResponseContent());
        map.put("createTime", manage.getCreateTime());
        map.put("completeTime", manage.getCompleteTime());
        String statusText = switch (manage.getStatus()) {
            case 0 -> "待审核";
            case 1 -> "已同意";
            case 2 -> "已拒绝";
            case 3 -> "待交接";
            case 4 -> "已完成";
            default -> "未知";
        };
        map.put("statusText", statusText);
        RentalContract contract = rentalContractMapper.selectById(manage.getContractId());
        if (contract != null) {
            map.put("contractId", contract.getContractId());
            map.put("contractNo", contract.getContractNo());
            map.put("depositAmount", contract.getDepositAmount());
            map.put("rentEndDate", contract.getRentEndDate());
            House house = houseMapper.selectById(contract.getHouseId());
            if (house != null) {
                map.put("houseTitle", house.getTitle());
                map.put("houseAddress", house.getAddress());
            }
            User landlord = userMapper.selectById(contract.getLandlordId());
            if (landlord != null) {
                map.put("landlordName", landlord.getRealName());
                map.put("landlordPhone", landlord.getPhone());
            }
        }
        return map;
    }
}
