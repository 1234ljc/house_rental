package com.example.demo.controller.rental;

import com.example.demo.entity.Result;
import com.example.demo.service.rental.TenantAfterService;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/tenant/after")
public class TenantAfterController {

    private final TenantAfterService tenantAfterService;

    public TenantAfterController(TenantAfterService tenantAfterService) {
        this.tenantAfterService = tenantAfterService;
    }

    @GetMapping("/rent/list")
    public Result getRentList(@RequestParam(required = false) Long contractId,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer size,
                              HttpServletRequest request) {
        return tenantAfterService.getRentList(contractId, page, size, request);
    }

    @GetMapping("/contracts")
    public Result getActiveContracts(HttpServletRequest request) {
        return tenantAfterService.getActiveContracts(request);
    }

    @PostMapping("/issue/submit")
    public Result submitIssue(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        return tenantAfterService.submitIssue(params, request);
    }

    @GetMapping("/issue/list")
    public Result getIssueList(@RequestParam(required = false) Integer status,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size,
                               HttpServletRequest request) {
        return tenantAfterService.getIssueList(status, page, size, request);
    }

    @PutMapping("/issue/append/{manageId}")
    public Result appendIssue(@PathVariable Long manageId, @RequestBody Map<String, String> params,
                              HttpServletRequest request) {
        return tenantAfterService.appendIssue(manageId, params, request);
    }

    @PostMapping("/checkout/apply")
    public Result applyCheckout(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        return tenantAfterService.applyCheckout(params, request);
    }

    @GetMapping("/checkout/list")
    public Result getCheckoutList(HttpServletRequest request) {
        return tenantAfterService.getCheckoutList(request);
    }

    @GetMapping("/checkout/{manageId}")
    public Result getCheckoutDetail(@PathVariable Long manageId, HttpServletRequest request) {
        return tenantAfterService.getCheckoutDetail(manageId, request);
    }

    @PutMapping("/checkout/cancel/{manageId}")
    public Result cancelCheckout(@PathVariable Long manageId, HttpServletRequest request) {
        return tenantAfterService.cancelCheckout(manageId, request);
    }

    @PutMapping("/checkout/confirm/{manageId}")
    public Result confirmCheckout(@PathVariable Long manageId, HttpServletRequest request) {
        return tenantAfterService.confirmCheckout(manageId, request);
    }
}
