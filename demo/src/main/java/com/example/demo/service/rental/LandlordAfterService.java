package com.example.demo.service.rental;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface LandlordAfterService{
    Result getCurrentTenants(HttpServletRequest request);

    Result getHistoryTenants(Integer page, Integer size, HttpServletRequest request);

    Result getTenantDetail(Long contractId, HttpServletRequest request);

    Result getIssueList(Integer status, Long houseId, Integer page, Integer size, HttpServletRequest request);

    Result getIssueStats(HttpServletRequest request);

    Result processIssue(Long manageId, Map<String, Object> params, HttpServletRequest request);

    Result getCheckoutList(Integer status, Integer page, Integer size, HttpServletRequest request);

    Result getCheckoutStats(HttpServletRequest request);

    Result auditCheckout(Long manageId, Map<String, Object> params, HttpServletRequest request);

    Result arrangeHandover(Long manageId, Map<String, Object> params, HttpServletRequest request);

    Result completeCheckout(Long manageId, Map<String, Object> params, HttpServletRequest request);
}
