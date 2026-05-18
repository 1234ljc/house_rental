package com.example.demo.service.rental.impl;

import lombok.extern.slf4j.Slf4j;
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
import com.example.demo.service.rental.TenantRentalService;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Base64;

@Service
// 负责租客侧合同查看、确认、下载和续租申请功能实现。
@Slf4j
public class TenantRentalServiceImpl implements TenantRentalService {

    private final RentalContractMapper rentalContractMapper;
    private final HouseMapper houseMapper;
    private final UserMapper userMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final NotificationService notificationService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public TenantRentalServiceImpl(RentalContractMapper rentalContractMapper,
                                   HouseMapper houseMapper,
                                   UserMapper userMapper,
                                   PaymentOrderMapper paymentOrderMapper,
                                   NotificationService notificationService) {
        this.rentalContractMapper = rentalContractMapper;
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.notificationService = notificationService;
    }

    // 租客合同概览：按状态统计自己的合同数量，给前端首页/仪表盘使用。
    @Override
    public Result getContractStats(HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        if (tenantId == null) {
            return Result.failure("请先登录");
        }

        List<Map<String, Object>> rows = rentalContractMapper.countByStatusForTenant(tenantId);
        Map<Integer, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Integer status = ((Number) row.get("status")).intValue();
            Long cnt = ((Number) row.get("cnt")).longValue();
            countMap.put(status, cnt);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingSign", countMap.getOrDefault(1, 0L));
        stats.put("active", countMap.getOrDefault(2, 0L));
        stats.put("expired", countMap.getOrDefault(3, 0L));
        return Result.success(stats);
    }

    // 租客合同列表：按状态分页查看自己的合同记录。
    @Override
    public Result getContractList(Integer status, Integer page, Integer size, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        if (tenantId == null) {
            return Result.failure("请先登录");
        }

        Page<RentalContract> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getTenantId, tenantId);
        if (status != null) {
            wrapper.eq(RentalContract::getStatus, status);
        } else {
            wrapper.ne(RentalContract::getStatus, 0);
        }
        wrapper.orderByDesc(RentalContract::getCreateTime);

        Page<RentalContract> result = rentalContractMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (RentalContract contract : result.getRecords()) {
            records.add(buildContractInfo(contract));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    // 合同详情流程：校验归属后返回合同、房源和房东的完整信息。
    @Override
    public Result getContractDetail(Long contractId, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        if (tenantId == null) {
            return Result.failure("请先登录");
        }

        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null || !contract.getTenantId().equals(tenantId)) {
            return Result.failure("合同不存在或无权查看");
        }
        return Result.success(buildContractInfo(contract));
    }

    // 身份核验流程：用于合同确认前的身份校验，防止非本人签署。
    @Override
    public Result verifyIdentity(Map<String, String> params, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        if (tenantId == null) {
            return Result.failure("请先登录");
        }

        String idCardLast6 = params.get("idCardLast6");
        String phoneLast4 = params.get("phoneLast4");
        if (idCardLast6 == null || idCardLast6.length() != 6) {
            return Result.failure("请输入正确的身份证后6位");
        }
        if (phoneLast4 == null || phoneLast4.length() != 4) {
            return Result.failure("请输入正确的手机号后4位");
        }

        User user = userMapper.selectById(tenantId);
        if (user == null) {
            return Result.failure("用户不存在");
        }
        if (user.getIdCard() == null || user.getIdCard().length() < 6) {
            return Result.failure("您尚未完成实名认证");
        }
        String realIdCardLast6 = user.getIdCard().substring(user.getIdCard().length() - 6);
        if (!realIdCardLast6.equalsIgnoreCase(idCardLast6)) {
            return Result.failure("身份证后6位不正确");
        }
        if (user.getPhone() == null || user.getPhone().length() < 4) {
            return Result.failure("您尚未绑定手机号");
        }
        String realPhoneLast4 = user.getPhone().substring(user.getPhone().length() - 4);
        if (!realPhoneLast4.equals(phoneLast4)) {
            return Result.failure("手机号后4位不正确");
        }
        return Result.success("身份验证通过");
    }

    // 合同确认流程：可选保存电子签名，确认后让合同生效并创建首期账单。
    @Override
    public Result confirmContract(Long contractId, Map<String, String> params, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        if (tenantId == null) {
            return Result.failure("请先登录");
        }

        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null || !contract.getTenantId().equals(tenantId)) {
            return Result.failure("合同不存在或无权操作");
        }
        if (contract.getStatus() != 1) {
            return Result.failure("合同状态不正确，无法确认");
        }
        if (contract.getTenantSignTime() != null) {
            return Result.failure("您已确认过该合同");
        }

        String signature = params != null ? params.get("signature") : null;
        if (signature != null && !signature.isEmpty()) {
            try {
                Path signaturePath = Paths.get(uploadDir, "signatures", tenantId.toString());
                if (!Files.exists(signaturePath)) {
                    Files.createDirectories(signaturePath);
                }
                String signatureFileName = "sign_" + contractId + "_tenant_" + System.currentTimeMillis() + ".png";
                Path signatureFile = signaturePath.resolve(signatureFileName);
                String base64Data = signature.contains(",") ? signature.split(",")[1] : signature;
                Files.write(signatureFile, Base64.getDecoder().decode(base64Data));
            } catch (Exception e) {
                log.error("签名保存失败", e);
            }
        }

        contract.setTenantSignTime(LocalDateTime.now());
        contract.setStatus(2);
        contract.setUpdateTime(LocalDateTime.now());
        rentalContractMapper.updateById(contract);

        House house = houseMapper.selectById(contract.getHouseId());
        if (house != null) {
            house.setStatus(2);
            houseMapper.updateById(house);
        }

        createFirstPaymentOrder(contract);
        String houseTitle = house != null ? house.getTitle() : "房源";
        notificationService.notifyContractEffective(tenantId, contract.getLandlordId(), houseTitle, contractId);
        return Result.success("合同确认成功");
    }

    // 合同下载流程：校验权限后返回合同文件流，供租客本地保存。
    @Override
    public ResponseEntity<Resource> downloadContract(Long contractId, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null) {
            return ResponseEntity.notFound().build();
        }
        if (!contract.getTenantId().equals(tenantId)) {
            return ResponseEntity.status(403).build();
        }
        if (contract.getContent() == null || !contract.getContent().contains("|")) {
            return ResponseEntity.notFound().build();
        }

        String[] parts = contract.getContent().split("\\|");
        String filePath = parts[0];
        String originalFilename = parts.length > 1 ? parts[1] : "contract.pdf";
        try {
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            String encodedFileName = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 续租申请流程：租客提交续租意向，等待房东后续审批。
    @Override
    public Result applyRenewal(Long contractId, HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        if (tenantId == null) {
            return Result.failure("请先登录");
        }

        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null || !contract.getTenantId().equals(tenantId)) {
            return Result.failure("合同不存在或无权操作");
        }
        if (contract.getStatus() != 2 && contract.getStatus() != 3) {
            return Result.failure("只有已确认或已到期的合同可以申请续租");
        }
        if (contract.getRenewalStatus() != null && contract.getRenewalStatus() == 1) {
            return Result.failure("已有续租申请在处理中");
        }

        contract.setRenewalStatus(1);
        contract.setUpdateTime(LocalDateTime.now());
        rentalContractMapper.updateById(contract);

        House house = houseMapper.selectById(contract.getHouseId());
        User tenant = userMapper.selectById(tenantId);
        String houseTitle = house != null ? house.getTitle() : "房源";
        String tenantName = tenant != null ? (tenant.getRealName() != null ? tenant.getRealName() : tenant.getUsername()) : "租客";
        notificationService.notifyRenewalApplied(contract.getLandlordId(), tenantName, houseTitle, contractId);
        return Result.success("续租申请已提交");
    }

    private Map<String, Object> buildContractInfo(RentalContract contract) {
        Map<String, Object> map = new HashMap<>();
        map.put("contractId", contract.getContractId());
        map.put("contractNo", contract.getContractNo());
        map.put("houseId", contract.getHouseId());
        map.put("rentStartDate", contract.getRentStartDate());
        map.put("rentEndDate", contract.getRentEndDate());
        map.put("monthlyRent", contract.getMonthlyRent());
        map.put("depositAmount", contract.getDepositAmount());
        map.put("paymentDay", contract.getPaymentDay());
        map.put("status", contract.getStatus());
        map.put("tenantSignTime", contract.getTenantSignTime());
        map.put("landlordSignTime", contract.getLandlordSignTime());
        map.put("renewalStatus", contract.getRenewalStatus());
        map.put("createTime", contract.getCreateTime());
        if (contract.getContent() != null && contract.getContent().contains("|")) {
            String[] parts = contract.getContent().split("\\|");
            map.put("hasFile", true);
            map.put("fileName", parts.length > 1 ? parts[1] : "合同文件");
        } else {
            map.put("hasFile", false);
        }
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
            landlordInfo.put("username", landlord.getUsername());
            landlordInfo.put("realName", landlord.getRealName());
            landlordInfo.put("phone", landlord.getPhone());
            landlordInfo.put("avatar", landlord.getAvatar());
            map.put("landlord", landlordInfo);
        }
        return map;
    }

    private void createFirstPaymentOrder(RentalContract contract) {
        LambdaQueryWrapper<PaymentOrder> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(PaymentOrder::getContractId, contract.getContractId()).eq(PaymentOrder::getOrderType, 0);
        if (paymentOrderMapper.selectCount(checkWrapper) > 0) {
            return;
        }

        BigDecimal totalAmount = contract.getMonthlyRent().add(contract.getDepositAmount());
        PaymentOrder order = new PaymentOrder();
        order.setContractId(contract.getContractId());
        order.setOrderNo(generateOrderNo("FP"));
        order.setOrderType(0);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(BigDecimal.ZERO);
        order.setPaymentStatus(0);
        order.setCreateTime(LocalDateTime.now());
        paymentOrderMapper.insert(order);
    }

    private String generateOrderNo(String prefix) {
        return prefix + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", new java.util.Random().nextInt(10000));
    }
}
