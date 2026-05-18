package com.example.demo.service.rental.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Result;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.repository.rental.entity.RentalContract;
import com.example.demo.repository.rental.entity.RentalManage;
import com.example.demo.repository.rental.RentalContractMapper;
import com.example.demo.repository.rental.RentalManageMapper;
import com.example.demo.service.rental.LandlordAfterService;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.UserMapper;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

@Service
// 负责房东侧当前租客、历史租客、报修工单和退租处理功能实现。
public class LandlordAfterServiceImpl implements LandlordAfterService {

    private final RentalManageMapper rentalManageMapper;
    private final RentalContractMapper rentalContractMapper;
    private final HouseMapper houseMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public LandlordAfterServiceImpl(RentalManageMapper rentalManageMapper,
                                    RentalContractMapper rentalContractMapper,
                                    HouseMapper houseMapper,
                                    UserMapper userMapper,
                                    NotificationService notificationService) {
        this.rentalManageMapper = rentalManageMapper;
        this.rentalContractMapper = rentalContractMapper;
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    // 当前租客列表：查看正在生效合同对应的租客，便于后续沟通和履约。
    @Override
    public Result getCurrentTenants(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getLandlordId, landlordId).eq(RentalContract::getStatus, 2).orderByDesc(RentalContract::getCreateTime);
        List<RentalContract> contracts = rentalContractMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (RentalContract contract : contracts) {
            result.add(buildTenantInfo(contract));
        }
        return Result.success(result);
    }

    // 历史租客列表：查看已到期或已终止合同中的租客记录。
    @Override
    public Result getHistoryTenants(Integer page, Integer size, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        Page<RentalContract> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getLandlordId, landlordId).in(RentalContract::getStatus, Arrays.asList(3, 4)).orderByDesc(RentalContract::getUpdateTime);
        Page<RentalContract> result = rentalContractMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (RentalContract contract : result.getRecords()) {
            records.add(buildTenantInfo(contract));
        }
        return Result.success(Map.of("records", records, "total", result.getTotal()));
    }

    @Override
    public Result getTenantDetail(Long contractId, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null || !contract.getLandlordId().equals(landlordId)) {
            return Result.failure("合同不存在或无权查看");
        }
        Map<String, Object> data = buildTenantInfo(contract);
        LambdaQueryWrapper<RentalManage> issueWrapper = new LambdaQueryWrapper<>();
        issueWrapper.eq(RentalManage::getContractId, contractId).orderByDesc(RentalManage::getCreateTime).last("LIMIT 5");
        data.put("recentIssues", rentalManageMapper.selectList(issueWrapper));
        return Result.success(data);
    }

    @Override
    public Result getIssueList(Integer status, Long houseId, Integer page, Integer size, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<RentalContract> cw = new LambdaQueryWrapper<>();
        cw.eq(RentalContract::getLandlordId, landlordId);
        if (houseId != null) {
            cw.eq(RentalContract::getHouseId, houseId);
        }
        cw.select(RentalContract::getContractId);
        List<RentalContract> contracts = rentalContractMapper.selectList(cw);
        List<Long> contractIds = contracts.stream().map(RentalContract::getContractId).toList();
        if (contractIds.isEmpty()) return Result.success(Map.of("records", new ArrayList<>(), "total", 0));

        Page<RentalManage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RentalManage> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RentalManage::getContractId, contractIds);
        if (status != null) wrapper.eq(RentalManage::getStatus, status);
        wrapper.orderByDesc(RentalManage::getCreateTime);
        Page<RentalManage> result = rentalManageMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (RentalManage manage : result.getRecords()) records.add(buildIssueInfo(manage));
        return Result.success(Map.of("records", records, "total", result.getTotal()));
    }

    @Override
    public Result getIssueStats(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<RentalContract> cw = new LambdaQueryWrapper<>();
        cw.eq(RentalContract::getLandlordId, landlordId).select(RentalContract::getContractId);
        List<RentalContract> contracts = rentalContractMapper.selectList(cw);
        List<Long> contractIds = contracts.stream().map(RentalContract::getContractId).toList();
        Map<String, Object> stats = new HashMap<>();
        if (contractIds.isEmpty()) {
            stats.put("pending", 0); stats.put("processing", 0); stats.put("completed", 0);
            return Result.success(stats);
        }
        LambdaQueryWrapper<RentalManage> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.in(RentalManage::getContractId, contractIds).eq(RentalManage::getStatus, 0);
        stats.put("pending", rentalManageMapper.selectCount(pendingWrapper));
        LambdaQueryWrapper<RentalManage> processingWrapper = new LambdaQueryWrapper<>();
        processingWrapper.in(RentalManage::getContractId, contractIds).eq(RentalManage::getStatus, 1);
        stats.put("processing", rentalManageMapper.selectCount(processingWrapper));
        LambdaQueryWrapper<RentalManage> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.in(RentalManage::getContractId, contractIds).eq(RentalManage::getStatus, 2);
        stats.put("completed", rentalManageMapper.selectCount(completedWrapper));
        return Result.success(stats);
    }

    @Override
    public Result processIssue(Long manageId, Map<String, Object> params, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        RentalManage manage = rentalManageMapper.selectById(manageId);
        if (manage == null) return Result.failure("记录不存在");
        RentalContract contract = rentalContractMapper.selectById(manage.getContractId());
        if (contract == null || !contract.getLandlordId().equals(landlordId)) return Result.failure("无权操作");
        Integer newStatus = params.get("status") != null ? Integer.valueOf(params.get("status").toString()) : 1;
        String response = (String) params.get("response");
        manage.setStatus(newStatus);
        if (response != null) manage.setResponseContent(response);
        if (newStatus == 2) manage.setCompleteTime(LocalDateTime.now());
        rentalManageMapper.updateById(manage);
        House house = houseMapper.selectById(contract.getHouseId());
        String houseTitle = house != null ? house.getTitle() : "房源";
        if (newStatus == 2) notificationService.notifyIssueResolved(manage.getUserId(), houseTitle, manageId);
        else notificationService.notifyIssueProcessed(manage.getUserId(), houseTitle, manageId);
        return Result.success(newStatus == 2 ? "问题已解决" : "已回复租客");
    }

    @Override
    public Result getCheckoutList(Integer status, Integer page, Integer size, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<RentalContract> cw = new LambdaQueryWrapper<>();
        cw.eq(RentalContract::getLandlordId, landlordId).select(RentalContract::getContractId);
        List<RentalContract> contracts = rentalContractMapper.selectList(cw);
        List<Long> contractIds = contracts.stream().map(RentalContract::getContractId).toList();
        if (contractIds.isEmpty()) return Result.success(Map.of("records", new ArrayList<>(), "total", 0));
        Page<RentalManage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RentalManage> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RentalManage::getContractId, contractIds).eq(RentalManage::getManageType, 2);
        if (status != null) wrapper.eq(RentalManage::getStatus, status);
        wrapper.orderByDesc(RentalManage::getCreateTime);
        Page<RentalManage> result = rentalManageMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (RentalManage manage : result.getRecords()) records.add(buildCheckoutInfo(manage));
        return Result.success(Map.of("records", records, "total", result.getTotal()));
    }

    @Override
    public Result getCheckoutStats(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<RentalContract> cw = new LambdaQueryWrapper<>();
        cw.eq(RentalContract::getLandlordId, landlordId).select(RentalContract::getContractId);
        List<RentalContract> contracts = rentalContractMapper.selectList(cw);
        List<Long> contractIds = contracts.stream().map(RentalContract::getContractId).toList();
        Map<String, Object> stats = new HashMap<>();
        if (contractIds.isEmpty()) {
            stats.put("pending", 0); stats.put("approved", 0); stats.put("handover", 0); stats.put("completed", 0);
            return Result.success(stats);
        }
        LambdaQueryWrapper<RentalManage> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.in(RentalManage::getContractId, contractIds).eq(RentalManage::getManageType, 2).eq(RentalManage::getStatus, 0);
        stats.put("pending", rentalManageMapper.selectCount(pendingWrapper));
        LambdaQueryWrapper<RentalManage> approvedWrapper = new LambdaQueryWrapper<>();
        approvedWrapper.in(RentalManage::getContractId, contractIds).eq(RentalManage::getManageType, 2).eq(RentalManage::getStatus, 1);
        stats.put("approved", rentalManageMapper.selectCount(approvedWrapper));
        LambdaQueryWrapper<RentalManage> handoverWrapper = new LambdaQueryWrapper<>();
        handoverWrapper.in(RentalManage::getContractId, contractIds).eq(RentalManage::getManageType, 2).eq(RentalManage::getStatus, 3);
        stats.put("handover", rentalManageMapper.selectCount(handoverWrapper));
        LambdaQueryWrapper<RentalManage> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.in(RentalManage::getContractId, contractIds).eq(RentalManage::getManageType, 2).eq(RentalManage::getStatus, 4);
        stats.put("completed", rentalManageMapper.selectCount(completedWrapper));
        return Result.success(stats);
    }

    @Override
    public Result auditCheckout(Long manageId, Map<String, Object> params, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        RentalManage manage = rentalManageMapper.selectById(manageId);
        if (manage == null || manage.getManageType() != 2) return Result.failure("退租申请不存在");
        RentalContract contract = rentalContractMapper.selectById(manage.getContractId());
        if (contract == null || !contract.getLandlordId().equals(landlordId)) return Result.failure("无权操作");
        if (manage.getStatus() != 0) return Result.failure("该申请已处理");
        Integer action = Integer.valueOf(params.get("action").toString());
        String response = (String) params.get("response");
        if (action == 1) {
            manage.setStatus(1);
            manage.setResponseContent(response != null ? response : "房东已同意您的退租申请，请等待安排交接");
        } else {
            manage.setStatus(2);
            manage.setResponseContent(response != null ? response : "房东拒绝了您的退租申请");
        }
        rentalManageMapper.updateById(manage);
        House house = houseMapper.selectById(contract.getHouseId());
        String houseTitle = house != null ? house.getTitle() : "房源";
        if (action == 1) notificationService.notifyCheckoutApproved(manage.getUserId(), houseTitle, manageId);
        else notificationService.notifyCheckoutRejected(manage.getUserId(), houseTitle, response, manageId);
        return Result.success(action == 1 ? "已同意退租申请" : "已拒绝退租申请");
    }

    @Override
    public Result arrangeHandover(Long manageId, Map<String, Object> params, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        RentalManage manage = rentalManageMapper.selectById(manageId);
        if (manage == null || manage.getManageType() != 2) return Result.failure("退租申请不存在");
        RentalContract contract = rentalContractMapper.selectById(manage.getContractId());
        if (contract == null || !contract.getLandlordId().equals(landlordId)) return Result.failure("无权操作");
        if (manage.getStatus() != 1) return Result.failure("只有已同意的申请才能安排交接");
        String handoverTime = (String) params.get("handoverTime");
        String handoverNote = (String) params.get("handoverNote");
        manage.setStatus(3);
        manage.setResponseContent(String.format("交接时间：%s\n备注：%s", handoverTime != null ? handoverTime : "待定", handoverNote != null ? handoverNote : "无"));
        rentalManageMapper.updateById(manage);
        House house = houseMapper.selectById(contract.getHouseId());
        notificationService.notifyHandoverArranged(manage.getUserId(), house != null ? house.getTitle() : "房源", handoverTime, manageId);
        return Result.success("已安排交接，等待租客确认");
    }

    @Override
    public Result completeCheckout(Long manageId, Map<String, Object> params, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        RentalManage manage = rentalManageMapper.selectById(manageId);
        if (manage == null || manage.getManageType() != 2) return Result.failure("退租申请不存在");
        RentalContract contract = rentalContractMapper.selectById(manage.getContractId());
        if (contract == null || !contract.getLandlordId().equals(landlordId)) return Result.failure("无权操作");
        if (manage.getStatus() != 3 && manage.getStatus() != 1) return Result.failure("当前状态不能完成退租");
        String damageDesc = (String) params.get("damageDesc");
        String deductReason = (String) params.get("deductReason");
        manage.setStatus(4);
        manage.setCompleteTime(LocalDateTime.now());
        StringBuilder response = new StringBuilder("退租已完成\n");
        if (damageDesc != null && !damageDesc.isEmpty()) response.append("房屋状况：").append(damageDesc).append("\n");
        if (deductReason != null && !deductReason.isEmpty()) response.append("押金处理：").append(deductReason);
        else response.append("押金处理：全额退还");
        manage.setResponseContent(response.toString());
        rentalManageMapper.updateById(manage);
        contract.setStatus(4);
        contract.setUpdateTime(LocalDateTime.now());
        rentalContractMapper.updateById(contract);
        House house = houseMapper.selectById(contract.getHouseId());
        if (house != null) {
            house.setStatus(1);
            houseMapper.updateById(house);
        }
        notificationService.notifyCheckoutComplete(manage.getUserId(), house != null ? house.getTitle() : "房源", manageId);
        return Result.success("退租已完成，合同已终止");
    }

    private Map<String, Object> buildTenantInfo(RentalContract contract) {
        Map<String, Object> map = new HashMap<>();
        map.put("contractId", contract.getContractId());
        map.put("contractNo", contract.getContractNo());
        map.put("status", contract.getStatus());
        map.put("rentStartDate", contract.getRentStartDate());
        map.put("rentEndDate", contract.getRentEndDate());
        map.put("monthlyRent", contract.getMonthlyRent());
        User tenant = userMapper.selectById(contract.getTenantId());
        if (tenant != null) {
            map.put("tenantId", tenant.getUserId());
            map.put("tenantName", tenant.getRealName());
            map.put("tenantPhone", tenant.getPhone());
            map.put("tenantAvatar", tenant.getAvatar());
        }
        House house = houseMapper.selectById(contract.getHouseId());
        if (house != null) {
            map.put("houseId", house.getHouseId());
            map.put("houseTitle", house.getTitle());
            map.put("houseAddress", house.getAddress());
            map.put("houseImages", house.getImages());
        }
        return map;
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
            User tenant = userMapper.selectById(contract.getTenantId());
            if (tenant != null) {
                map.put("tenantName", tenant.getRealName());
                map.put("tenantPhone", tenant.getPhone());
            }
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
            User tenant = userMapper.selectById(contract.getTenantId());
            if (tenant != null) {
                map.put("tenantId", tenant.getUserId());
                map.put("tenantName", tenant.getRealName());
                map.put("tenantPhone", tenant.getPhone());
            }
        }
        return map;
    }
}
