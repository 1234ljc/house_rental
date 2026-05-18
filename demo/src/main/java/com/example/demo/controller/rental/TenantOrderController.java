package com.example.demo.controller.rental;

import com.example.demo.entity.Result;
import com.example.demo.service.rental.TenantOrderService;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/tenant/order")
public class TenantOrderController {

    private final TenantOrderService tenantOrderService;

    public TenantOrderController(TenantOrderService tenantOrderService) {
        this.tenantOrderService = tenantOrderService;
    }

    @GetMapping("/stats")
    public Result getOrderStats(HttpServletRequest request) {
        return tenantOrderService.getOrderStats(request);
    }

    @GetMapping("/beans")
    public Result getBeansInfo(HttpServletRequest request) {
        return tenantOrderService.getBeansInfo(request);
    }

    @GetMapping("/calc-beans/{orderId}")
    public Result calcBeansForOrder(@PathVariable Long orderId, HttpServletRequest request) {
        return tenantOrderService.calcBeansForOrder(orderId, request);
    }

    @GetMapping("/list")
    public Result getOrderList(@RequestParam(required = false) Integer orderType,
                               @RequestParam(required = false) Integer paymentStatus,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size,
                               HttpServletRequest request) {
        return tenantOrderService.getOrderList(orderType, paymentStatus, page, size, request);
    }

    @GetMapping("/{orderId}")
    public Result getOrderDetail(@PathVariable Long orderId, HttpServletRequest request) {
        return tenantOrderService.getOrderDetail(orderId, request);
    }

    @PostMapping("/pay/{orderId}")
    public Result payOrder(@PathVariable Long orderId, @RequestBody Map<String, Object> params,
                           HttpServletRequest request) {
        return tenantOrderService.payOrder(orderId, params, request);
    }

    @PostMapping("/deposit-refund/{contractId}")
    public Result applyDepositRefund(@PathVariable Long contractId,
                                     @RequestBody(required = false) Map<String, String> params,
                                     HttpServletRequest request) {
        return tenantOrderService.applyDepositRefund(contractId, params, request);
    }

    @GetMapping("/pending")
    public Result getPendingOrders(HttpServletRequest request) {
        return tenantOrderService.getPendingOrders(request);
    }

    @PostMapping("/init-orders")
    public Result initOrdersForExistingContracts(HttpServletRequest request) {
        return tenantOrderService.initOrdersForExistingContracts(request);
    }
}
