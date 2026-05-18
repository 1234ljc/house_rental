package com.example.demo.service.rental;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface TenantAfterService{
    Result getRentList(Long contractId, Integer page, Integer size, HttpServletRequest request);

    Result getActiveContracts(HttpServletRequest request);

    Result submitIssue(Map<String, Object> params, HttpServletRequest request);

    Result getIssueList(Integer status, Integer page, Integer size, HttpServletRequest request);

    Result appendIssue(Long manageId, Map<String, String> params, HttpServletRequest request);

    Result applyCheckout(Map<String, Object> params, HttpServletRequest request);

    Result getCheckoutList(HttpServletRequest request);

    Result getCheckoutDetail(Long manageId, HttpServletRequest request);

    Result cancelCheckout(Long manageId, HttpServletRequest request);

    Result confirmCheckout(Long manageId, HttpServletRequest request);
}
