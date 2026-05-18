package com.example.demo.service.rental.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Result;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.repository.rental.entity.RentalContract;
import com.example.demo.repository.rental.RentalContractMapper;
import com.example.demo.service.rental.AdminRentalService;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminRentalServiceImpl implements AdminRentalService {

    private final RentalContractMapper contractMapper;
    private final HouseMapper houseMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public AdminRentalServiceImpl(RentalContractMapper contractMapper,
                                  HouseMapper houseMapper,
                                  UserMapper userMapper,
                                  NotificationService notificationService) {
        this.contractMapper = contractMapper;
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Override
    public Result getContractList(Integer status, String keyword, Integer page, Integer size) {
        Page<RentalContract> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        if (status != null && status != -1) {
            wrapper.eq(RentalContract::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(RentalContract::getContractNo, keyword);
        }
        wrapper.orderByDesc(RentalContract::getCreateTime);
        Page<RentalContract> result = contractMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (RentalContract contract : result.getRecords()) records.add(buildContractInfo(contract));
        return Result.success(Map.of("records", records, "total", result.getTotal(), "current", result.getCurrent(), "size", result.getSize()));
    }

    @Override
    public Result getContractDetail(Long id) {
        RentalContract contract = contractMapper.selectById(id);
        if (contract == null) {
            return Result.failure("合同不存在");
        }
        return Result.success(buildContractInfo(contract));
    }

    @Override
    public Result getContractStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", contractMapper.selectCount(null));
        stats.put("draft", contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 0)));
        stats.put("pending", contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 1)));
        stats.put("active", contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 2)));
        stats.put("expired", contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 3)));
        stats.put("terminated", contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 4)));
        return Result.success(stats);
    }

    @Override
    public Result terminateContract(Long id, Map<String, String> body) {
        String reason = body.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            return Result.failure("请填写终止原因");
        }
        RentalContract contract = contractMapper.selectById(id);
        if (contract == null) {
            return Result.failure("合同不存在");
        }
        if (contract.getStatus() == 4) {
            return Result.failure("合同已终止");
        }
        LambdaUpdateWrapper<RentalContract> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(RentalContract::getContractId, id).set(RentalContract::getStatus, 4).set(RentalContract::getUpdateTime, LocalDateTime.now());
        contractMapper.update(null, wrapper);
        notificationService.send(contract.getLandlordId(), NotificationService.TYPE_CONTRACT, "合同终止通知", "您的合同【" + contract.getContractNo() + "】已被管理员终止，原因：" + reason, contract.getContractId());
        notificationService.send(contract.getTenantId(), NotificationService.TYPE_CONTRACT, "合同终止通知", "您的合同【" + contract.getContractNo() + "】已被管理员终止，原因：" + reason, contract.getContractId());
        return Result.success("合同已终止");
    }

    private Map<String, Object> buildContractInfo(RentalContract contract) {
        Map<String, Object> map = new HashMap<>();
        map.put("contractId", contract.getContractId());
        map.put("contractNo", contract.getContractNo());
        map.put("houseId", contract.getHouseId());
        map.put("landlordId", contract.getLandlordId());
        map.put("tenantId", contract.getTenantId());
        map.put("content", contract.getContent());
        map.put("rentStartDate", contract.getRentStartDate());
        map.put("rentEndDate", contract.getRentEndDate());
        map.put("monthlyRent", contract.getMonthlyRent());
        map.put("depositAmount", contract.getDepositAmount());
        map.put("paymentDay", contract.getPaymentDay());
        map.put("status", contract.getStatus());
        map.put("tenantSignTime", contract.getTenantSignTime());
        map.put("landlordSignTime", contract.getLandlordSignTime());
        map.put("createTime", contract.getCreateTime());
        House house = houseMapper.selectById(contract.getHouseId());
        if (house != null) {
            map.put("houseTitle", house.getTitle());
            map.put("houseAddress", house.getAddress());
            map.put("houseImages", house.getImages());
        }
        User landlord = userMapper.selectById(contract.getLandlordId());
        if (landlord != null) {
            map.put("landlordName", landlord.getRealName() != null ? landlord.getRealName() : landlord.getUsername());
            map.put("landlordPhone", landlord.getPhone());
        }
        User tenant = userMapper.selectById(contract.getTenantId());
        if (tenant != null) {
            map.put("tenantName", tenant.getRealName() != null ? tenant.getRealName() : tenant.getUsername());
            map.put("tenantPhone", tenant.getPhone());
        }
        return map;
    }
}
