package com.example.demo.service.rental;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface TenantRentalService{
    Result getContractStats(HttpServletRequest request);

    Result getContractList(Integer status, Integer page, Integer size, HttpServletRequest request);

    Result getContractDetail(Long contractId, HttpServletRequest request);

    Result verifyIdentity(Map<String, String> params, HttpServletRequest request);

    Result confirmContract(Long contractId, Map<String, String> params, HttpServletRequest request);

    ResponseEntity<Resource> downloadContract(Long contractId, HttpServletRequest request);

    Result applyRenewal(Long contractId, HttpServletRequest request);
}
