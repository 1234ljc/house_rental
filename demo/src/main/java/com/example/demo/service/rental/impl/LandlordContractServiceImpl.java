package com.example.demo.service.rental.impl;

import com.example.demo.entity.Result;
import com.example.demo.service.rental.LandlordContractService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Service
public class LandlordContractServiceImpl implements LandlordContractService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public Result getMyContracts(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> contracts = new ArrayList<>();
        Path contractDir = Paths.get(uploadDir, "landlord-contracts", landlordId.toString());
        if (Files.exists(contractDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(contractDir)) {
                for (Path file : stream) {
                    if (Files.isRegularFile(file)) {
                        String fileName = file.getFileName().toString();
                        String[] parts = fileName.split("_", 3);
                        if (parts.length >= 3) {
                            Map<String, Object> contract = new HashMap<>();
                            contract.put("id", parts[0]);
                            contract.put("name", parts[1]);
                            contract.put("fileName", parts[2]);
                            contract.put("fileSize", Files.size(file));
                            contract.put("createTime", Files.getLastModifiedTime(file).toInstant().toString());
                            contracts.add(contract);
                        }
                    }
                }
            } catch (IOException ignored) {}
        }
        contracts.sort((a, b) -> ((String) b.get("createTime")).compareTo((String) a.get("createTime")));
        return Result.success(contracts);
    }

    @Override
    public Result uploadContract(MultipartFile file, String name, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        if (file.isEmpty()) {
            return Result.failure("请选择文件");
        }
        String originalFilename = file.getOriginalFilename();
        String fileType = getFileExtension(originalFilename);
        if (!isAllowedFileType(fileType)) {
            return Result.failure("只支持 doc, docx, pdf 格式的文件");
        }
        try {
            Path contractDir = Paths.get(uploadDir, "landlord-contracts", landlordId.toString());
            if (!Files.exists(contractDir)) {
                Files.createDirectories(contractDir);
            }
            String id = UUID.randomUUID().toString().substring(0, 8);
            String safeName = name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "");
            String newFileName = id + "_" + safeName + "_" + originalFilename;
            Files.copy(file.getInputStream(), contractDir.resolve(newFileName));
            return Result.success(id);
        } catch (IOException e) {
            return Result.failure("文件上传失败：" + e.getMessage());
        }
    }

    @Override
    public Result deleteContract(String id, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        Path contractDir = Paths.get(uploadDir, "landlord-contracts", landlordId.toString());
        try {
            if (Files.exists(contractDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(contractDir, id + "_*")) {
                    for (Path file : stream) {
                        Files.delete(file);
                        return Result.success("删除成功");
                    }
                }
            }
        } catch (IOException e) {
            return Result.failure("删除失败：" + e.getMessage());
        }
        return Result.failure("文件不存在");
    }

    @Override
    public ResponseEntity<Resource> downloadContract(String id, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        Path contractDir = Paths.get(uploadDir, "landlord-contracts", landlordId.toString());
        try {
            if (Files.exists(contractDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(contractDir, id + "_*")) {
                    for (Path file : stream) {
                        Resource resource = new UrlResource(file.toUri());
                        if (resource.exists()) {
                            String fileName = file.getFileName().toString();
                            String[] parts = fileName.split("_", 3);
                            String originalName = parts.length >= 3 ? parts[2] : fileName;
                            String encodedFileName = URLEncoder.encode(originalName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
                            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName).body(resource);
                        }
                    }
                }
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.notFound().build();
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }

    private boolean isAllowedFileType(String fileType) {
        return "doc".equals(fileType) || "docx".equals(fileType) || "pdf".equals(fileType);
    }
}
