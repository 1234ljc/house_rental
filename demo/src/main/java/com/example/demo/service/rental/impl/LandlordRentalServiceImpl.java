package com.example.demo.service.rental.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Result;
import com.example.demo.repository.chat.entity.ChatSession;
import com.example.demo.repository.chat.ChatSessionMapper;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.repository.rental.entity.PaymentOrder;
import com.example.demo.repository.rental.entity.RentalContract;
import com.example.demo.repository.rental.PaymentOrderMapper;
import com.example.demo.repository.rental.RentalContractMapper;
import com.example.demo.service.rental.LandlordRentalService;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
// 负责房东侧合同发起、更新、发送、续租审批和租客检索功能实现。
public class LandlordRentalServiceImpl implements LandlordRentalService {

    private final RentalContractMapper rentalContractMapper;
    private final HouseMapper houseMapper;
    private final UserMapper userMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final NotificationService notificationService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public LandlordRentalServiceImpl(RentalContractMapper rentalContractMapper,
                                     HouseMapper houseMapper,
                                     UserMapper userMapper,
                                     PaymentOrderMapper paymentOrderMapper,
                                     ChatSessionMapper chatSessionMapper,
                                     NotificationService notificationService) {
        this.rentalContractMapper = rentalContractMapper;
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.notificationService = notificationService;
    }

    // 房东合同概览：统计草稿、待确认、生效、到期和终止合同数量。
    @Override
    public Result getContractStats(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> rows = rentalContractMapper.countByStatusForLandlord(landlordId);
        Map<Integer, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Integer status = ((Number) row.get("status")).intValue();
            Long cnt = ((Number) row.get("cnt")).longValue();
            countMap.put(status, cnt);
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("draft", countMap.getOrDefault(0, 0L));
        stats.put("pending", countMap.getOrDefault(1, 0L));
        stats.put("active", countMap.getOrDefault(2, 0L));
        stats.put("expired", countMap.getOrDefault(3, 0L));
        stats.put("terminated", countMap.getOrDefault(4, 0L));
        return Result.success(stats);
    }

    // 房东合同列表：按房源、状态和关键字筛选自己名下的合同。
    @Override
    public Result getContractList(Integer status, Long houseId, String keyword, Integer page, Integer size, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        Page<RentalContract> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getLandlordId, landlordId);
        if (status != null) {
            wrapper.eq(RentalContract::getStatus, status);
        }
        if (houseId != null) {
            wrapper.eq(RentalContract::getHouseId, houseId);
        }
        wrapper.orderByDesc(RentalContract::getCreateTime);

        Page<RentalContract> result = rentalContractMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (RentalContract contract : result.getRecords()) {
            Map<String, Object> map = buildContractInfo(contract, keyword);
            if (map != null) records.add(map);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    // 合同详情流程：房东只能查看自己名下合同的完整信息。
    @Override
    public Result getContractDetail(Long contractId, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null || !contract.getLandlordId().equals(landlordId)) {
            return Result.failure("合同不存在或无权查看");
        }
        return Result.success(buildContractInfo(contract, null));
    }

    // 直接发起合同流程：房东上传合同文件并绑定房源、租客和租期信息。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createContractDirect(MultipartFile file, Long houseId, Long tenantId, String rentStartDateStr, Integer rentMonths, String monthlyRentStr, String depositAmountStr, Integer paymentDay, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        if (file.isEmpty()) {
            return Result.failure("请上传合同文件");
        }
        House house = houseMapper.selectById(houseId);
        if (house == null || !house.getLandlordId().equals(landlordId)) {
            return Result.failure("房源不存在或无权操作");
        }
        User tenant = userMapper.selectById(tenantId);
        if (tenant == null) {
            return Result.failure("租客不存在");
        }
        try {
            Path uploadPath = Paths.get(uploadDir, "contracts", landlordId.toString());
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String originalFilename = file.getOriginalFilename();
            String fileType = getFileExtension(originalFilename);
            if (!isAllowedFileType(fileType)) {
                return Result.failure("只支持 doc, docx, pdf 格式的文件");
            }
            String newFileName = UUID.randomUUID() + "." + fileType;
            Path filePath = uploadPath.resolve(newFileName);
            Files.copy(file.getInputStream(), filePath);

            LocalDate startDate = LocalDate.parse(rentStartDateStr);
            LocalDate endDate = startDate.plusMonths(rentMonths).minusDays(1);
            BigDecimal monthlyRent = StrUtil.isNotEmpty(monthlyRentStr) ? new BigDecimal(monthlyRentStr) : house.getRentPrice();
            BigDecimal deposit = StrUtil.isNotEmpty(depositAmountStr) ? new BigDecimal(depositAmountStr) : monthlyRent;

            RentalContract contract = new RentalContract();
            contract.setHouseId(houseId);
            contract.setLandlordId(landlordId);
            contract.setTenantId(tenantId);
            contract.setContractNo("HT" + System.currentTimeMillis());
            contract.setContent(filePath + "|" + originalFilename);
            contract.setRentStartDate(startDate);
            contract.setRentEndDate(endDate);
            contract.setMonthlyRent(monthlyRent);
            contract.setDepositAmount(deposit);
            contract.setPaymentDay(paymentDay);
            contract.setStatus(1);
            contract.setCreateTime(LocalDateTime.now());
            rentalContractMapper.insert(contract);
            notificationService.notifyContractSent(tenantId, house.getTitle(), contract.getContractId());
            return Result.success(contract.getContractId());
        } catch (IOException e) {
            return Result.failure("文件上传失败：" + e.getMessage());
        }
    }

    // 租客搜索流程：按姓名、手机号、用户名模糊检索可签约租客。
    @Override
    public Result searchTenant(String keyword, HttpServletRequest request) {
        if (StrUtil.isBlank(keyword) || keyword.trim().length() < 2) {
            return Result.failure("请输入至少2个字符");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserType, 1).and(w -> w.like(User::getRealName, keyword).or().like(User::getPhone, keyword).or().like(User::getUsername, keyword)).last("LIMIT 10");
        List<User> users = userMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", u.getUserId());
            item.put("username", u.getUsername());
            item.put("realName", u.getRealName());
            item.put("phone", u.getPhone() != null ? u.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2") : "");
            item.put("avatar", u.getAvatar());
            result.add(item);
        }
        return Result.success(result);
    }

    // 聊天租客列表：提取当前房东曾经联系过的租客，便于发起二次沟通。
    @Override
    public Result getChatTenants(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getLandlordId, landlordId).eq(ChatSession::getSessionType, 0).isNotNull(ChatSession::getTenantId).orderByDesc(ChatSession::getLastMessageTime);
        List<ChatSession> sessions = chatSessionMapper.selectList(wrapper);
        Set<Long> seen = new LinkedHashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatSession s : sessions) {
            if (seen.add(s.getTenantId())) {
                User tenant = userMapper.selectById(s.getTenantId());
                if (tenant != null && tenant.getUserType() == 1) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("userId", tenant.getUserId());
                    item.put("username", tenant.getUsername());
                    item.put("realName", tenant.getRealName());
                    item.put("phone", tenant.getPhone());
                    item.put("avatar", tenant.getAvatar());
                    result.add(item);
                }
            }
        }
        return Result.success(result);
    }

    // 合同更新流程：在草稿/待确认状态下修改租金、押金和付款日等基础信息。
    @Override
    public Result updateContract(Long contractId, Map<String, Object> params, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null || !contract.getLandlordId().equals(landlordId)) {
            return Result.failure("合同不存在或无权操作");
        }
        if (contract.getStatus() != 0 && contract.getStatus() != 1) {
            return Result.failure("只能修改草稿或待确认状态的合同");
        }
        if (params.containsKey("paymentDay")) {
            contract.setPaymentDay(Integer.valueOf(params.get("paymentDay").toString()));
        }
        if (params.containsKey("monthlyRent")) {
            contract.setMonthlyRent(new BigDecimal(params.get("monthlyRent").toString()));
        }
        if (params.containsKey("depositAmount")) {
            contract.setDepositAmount(new BigDecimal(params.get("depositAmount").toString()));
        }
        contract.setUpdateTime(LocalDateTime.now());
        rentalContractMapper.updateById(contract);
        return Result.success("合同已更新");
    }

    // 重新上传合同流程：替换旧合同文件并同步更新合同内容路径。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result reuploadContract(Long contractId, MultipartFile file, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null || !contract.getLandlordId().equals(landlordId)) {
            return Result.failure("合同不存在或无权操作");
        }
        if (contract.getStatus() != 0 && contract.getStatus() != 1) {
            return Result.failure("只能修改草稿或待确认状态的合同");
        }
        if (file.isEmpty()) {
            return Result.failure("请上传合同文件");
        }
        try {
            if (StrUtil.isNotEmpty(contract.getContent()) && contract.getContent().contains("|")) {
                try {
                    Files.deleteIfExists(Paths.get(contract.getContent().split("\\|")[0]));
                } catch (Exception ignored) {
                }
            }
            Path uploadPath = Paths.get(uploadDir, "contracts", landlordId.toString());
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String originalFilename = file.getOriginalFilename();
            String fileType = getFileExtension(originalFilename);
            if (!isAllowedFileType(fileType)) {
                return Result.failure("只支持 doc, docx, pdf 格式的文件");
            }
            String newFileName = UUID.randomUUID() + "." + fileType;
            Path filePath = uploadPath.resolve(newFileName);
            Files.copy(file.getInputStream(), filePath);
            contract.setContent(filePath + "|" + originalFilename);
            contract.setUpdateTime(LocalDateTime.now());
            rentalContractMapper.updateById(contract);
            return Result.success("合同已重新上传并发送给租客");
        } catch (IOException e) {
            return Result.failure("文件上传失败：" + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Resource> downloadContract(Long contractId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null) {
            return ResponseEntity.notFound().build();
        }
        if (!contract.getLandlordId().equals(userId) && !contract.getTenantId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        if (StrUtil.isEmpty(contract.getContent()) || !contract.getContent().contains("|")) {
            return ResponseEntity.notFound().build();
        }
        try {
            String[] parts = contract.getContent().split("\\|");
            Path path = Paths.get(parts[0]);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();
            String encodedFileName = URLEncoder.encode(parts.length > 1 ? parts[1] : "contract.pdf", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName).body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 合同发送流程：把草稿合同提交为待租客确认状态。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result sendContract(Long contractId, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null || !contract.getLandlordId().equals(landlordId)) {
            return Result.failure("合同不存在或无权操作");
        }
        if (contract.getStatus() != 0) {
            return Result.failure("只能发送草稿状态的合同");
        }
        contract.setStatus(1);
        contract.setUpdateTime(LocalDateTime.now());
        rentalContractMapper.updateById(contract);
        House house = houseMapper.selectById(contract.getHouseId());
        notificationService.notifyContractSent(contract.getTenantId(), house != null ? house.getTitle() : "房源", contractId);
        return Result.success("合同已发送，等待租客确认");
    }

    // 身份核验流程：房东在生成/确认合同前先校验本人身份信息。
    @Override
    public Result verifyIdentity(Map<String, String> params, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        if (landlordId == null) {
            return Result.failure("请先登录");
        }
        String idCardLast6 = params.get("idCardLast6");
        String phoneLast4 = params.get("phoneLast4");
        if (StrUtil.isEmpty(idCardLast6) || idCardLast6.length() != 6) {
            return Result.failure("请输入正确的身份证后6位");
        }
        if (StrUtil.isEmpty(phoneLast4) || phoneLast4.length() != 4) {
            return Result.failure("请输入正确的手机号后4位");
        }
        User user = userMapper.selectById(landlordId);
        if (user == null) {
            return Result.failure("用户不存在");
        }
        if (StrUtil.isEmpty(user.getIdCard()) || user.getIdCard().length() < 6) {
            return Result.failure("您尚未完成实名认证");
        }
        String realIdCardLast6 = StrUtil.sub(user.getIdCard(), -6, user.getIdCard().length());
        if (!realIdCardLast6.equalsIgnoreCase(idCardLast6)) {
            return Result.failure("身份证后6位不正确");
        }
        if (StrUtil.isEmpty(user.getPhone()) || user.getPhone().length() < 4) {
            return Result.failure("您尚未绑定手机号");
        }
        String realPhoneLast4 = StrUtil.sub(user.getPhone(), -4, user.getPhone().length());
        if (!realPhoneLast4.equals(phoneLast4)) {
            return Result.failure("手机号后4位不正确");
        }
        return Result.success("身份验证通过");
    }

    // 续租审批流程：房东上传新合同并生成续租合同，完成旧合同的续租状态流转。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result approveRenewal(Long contractId, MultipartFile file, String rentStartDateStr, Integer rentMonths, String monthlyRentStr, String depositAmountStr, Integer paymentDay, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        RentalContract oldContract = rentalContractMapper.selectById(contractId);
        if (oldContract == null || !oldContract.getLandlordId().equals(landlordId)) {
            return Result.failure("合同不存在或无权操作");
        }
        if (oldContract.getRenewalStatus() == null || oldContract.getRenewalStatus() != 1) {
            return Result.failure("该合同没有待处理的续租申请");
        }
        if (file.isEmpty()) {
            return Result.failure("请上传新合同文件");
        }
        try {
            Path uploadPath = Paths.get(uploadDir, "contracts", landlordId.toString());
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String originalFilename = file.getOriginalFilename();
            String fileType = getFileExtension(originalFilename);
            if (!isAllowedFileType(fileType)) return Result.failure("只支持 doc, docx, pdf 格式的文件");
            String newFileName = UUID.randomUUID() + "." + fileType;
            Path filePath = uploadPath.resolve(newFileName);
            Files.copy(file.getInputStream(), filePath);
            LocalDate startDate = LocalDate.parse(rentStartDateStr);
            LocalDate endDate = startDate.plusMonths(rentMonths).minusDays(1);
            House house = houseMapper.selectById(oldContract.getHouseId());
            BigDecimal monthlyRent = StrUtil.isNotEmpty(monthlyRentStr) ? new BigDecimal(monthlyRentStr) : (house != null ? house.getRentPrice() : oldContract.getMonthlyRent());
            BigDecimal deposit = StrUtil.isNotEmpty(depositAmountStr) ? new BigDecimal(depositAmountStr) : oldContract.getDepositAmount();
            RentalContract newContract = new RentalContract();
            newContract.setHouseId(oldContract.getHouseId());
            newContract.setLandlordId(landlordId);
            newContract.setTenantId(oldContract.getTenantId());
            newContract.setContractNo("HT" + System.currentTimeMillis());
            newContract.setContent(filePath + "|" + originalFilename);
            newContract.setRentStartDate(startDate);
            newContract.setRentEndDate(endDate);
            newContract.setMonthlyRent(monthlyRent);
            newContract.setDepositAmount(deposit);
            newContract.setPaymentDay(paymentDay);
            newContract.setStatus(1);
            newContract.setParentContractId(contractId);
            newContract.setCreateTime(LocalDateTime.now());
            rentalContractMapper.insert(newContract);
            oldContract.setRenewalStatus(2);
            oldContract.setUpdateTime(LocalDateTime.now());
            rentalContractMapper.updateById(oldContract);
            notificationService.notifyRenewalApproved(oldContract.getTenantId(), house != null ? house.getTitle() : "房源", newContract.getContractId());
            return Result.success(newContract.getContractId());
        } catch (IOException e) {
            return Result.failure("文件上传失败：" + e.getMessage());
        }
    }

    // 续租驳回流程：房东拒绝续租申请并通知租客原因。
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result rejectRenewal(Long contractId, Map<String, String> params, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null || !contract.getLandlordId().equals(landlordId)) return Result.failure("合同不存在或无权操作");
        if (contract.getRenewalStatus() == null || contract.getRenewalStatus() != 1) return Result.failure("该合同没有待处理的续租申请");
        contract.setRenewalStatus(3);
        contract.setUpdateTime(LocalDateTime.now());
        rentalContractMapper.updateById(contract);
        House house = houseMapper.selectById(contract.getHouseId());
        String reason = params != null ? params.get("reason") : null;
        notificationService.notifyRenewalRejected(contract.getTenantId(), house != null ? house.getTitle() : "房源", reason, contractId);
        return Result.success("已拒绝续租申请");
    }

    @Override
    public Result signContract(Long contractId, Map<String, String> params, HttpServletRequest request) {
        return Result.failure("合同已在线下签署，无需在平台上再次签署");
    }

    private String getFileExtension(String filename) {
        if (StrUtil.isEmpty(filename)) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }

    private boolean isAllowedFileType(String fileType) {
        return "doc".equals(fileType) || "docx".equals(fileType) || "pdf".equals(fileType);
    }

    private Map<String, Object> buildContractInfo(RentalContract contract, String keyword) {
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
        if (StrUtil.isNotEmpty(contract.getContent()) && contract.getContent().contains("|")) {
            String[] parts = contract.getContent().split("\\|");
            map.put("hasFile", true);
            map.put("fileName", parts.length > 1 ? parts[1] : "合同文件");
        } else {
            map.put("hasFile", false);
            map.put("content", contract.getContent());
        }
        House house = houseMapper.selectById(contract.getHouseId());
        if (house != null) {
            Map<String, Object> houseInfo = new HashMap<>();
            houseInfo.put("houseId", house.getHouseId());
            houseInfo.put("title", house.getTitle());
            houseInfo.put("address", house.getAddress());
            map.put("house", houseInfo);
        }
        User tenant = userMapper.selectById(contract.getTenantId());
        if (tenant != null) {
            if (StrUtil.isNotEmpty(keyword)) {
                String name = tenant.getRealName() != null ? tenant.getRealName() : tenant.getUsername();
                if (!name.contains(keyword) && !contract.getContractNo().contains(keyword)) return null;
            }
            Map<String, Object> tenantInfo = new HashMap<>();
            tenantInfo.put("userId", tenant.getUserId());
            tenantInfo.put("username", tenant.getUsername());
            tenantInfo.put("realName", tenant.getRealName());
            tenantInfo.put("phone", tenant.getPhone());
            tenantInfo.put("avatar", tenant.getAvatar());
            tenantInfo.put("idCard", tenant.getIdCard());
            map.put("tenant", tenantInfo);
        }
        return map;
    }
}
