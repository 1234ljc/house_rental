package com.example.demo.service.rental;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface LandlordRentalService{
    Result getContractStats(HttpServletRequest request);

    Result getContractList(Integer status, Long houseId, String keyword, Integer page, Integer size, HttpServletRequest request);

    Result getContractDetail(Long contractId, HttpServletRequest request);

    Result createContractDirect(MultipartFile file, Long houseId, Long tenantId, String rentStartDateStr, Integer rentMonths,
                                String monthlyRentStr, String depositAmountStr, Integer paymentDay, HttpServletRequest request);

    Result searchTenant(String keyword, HttpServletRequest request);

    Result getChatTenants(HttpServletRequest request);

    Result updateContract(Long contractId, Map<String, Object> params, HttpServletRequest request);

    Result reuploadContract(Long contractId, MultipartFile file, HttpServletRequest request);

    ResponseEntity<Resource> downloadContract(Long contractId, HttpServletRequest request);

    Result sendContract(Long contractId, HttpServletRequest request);

    Result verifyIdentity(Map<String, String> params, HttpServletRequest request);

    Result approveRenewal(Long contractId, MultipartFile file, String rentStartDateStr, Integer rentMonths,
                          String monthlyRentStr, String depositAmountStr, Integer paymentDay, HttpServletRequest request);

    Result rejectRenewal(Long contractId, Map<String, String> params, HttpServletRequest request);

    Result signContract(Long contractId, Map<String, String> params, HttpServletRequest request);
}
