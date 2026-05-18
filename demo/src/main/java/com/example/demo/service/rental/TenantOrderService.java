package com.example.demo.service.rental;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface TenantOrderService{
    Result getOrderStats(HttpServletRequest request);

    Result getBeansInfo(HttpServletRequest request);

    Result calcBeansForOrder(Long orderId, HttpServletRequest request);

    Result getOrderList(Integer orderType, Integer paymentStatus, Integer page, Integer size, HttpServletRequest request);

    Result getOrderDetail(Long orderId, HttpServletRequest request);

    Result payOrder(Long orderId, Map<String, Object> params, HttpServletRequest request);

    Result applyDepositRefund(Long contractId, Map<String, String> params, HttpServletRequest request);

    Result getPendingOrders(HttpServletRequest request);

    Result initOrdersForExistingContracts(HttpServletRequest request);
}
