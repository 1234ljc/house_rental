package com.example.demo.controller.rental;

import com.example.demo.entity.Result;
import com.example.demo.service.rental.LandlordAfterService;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/landlord/after")
public class LandlordAfterController {

    private final LandlordAfterService landlordAfterService;

    public LandlordAfterController(LandlordAfterService landlordAfterService) {
        this.landlordAfterService = landlordAfterService;
    }

    @GetMapping("/tenant/current")
    public Result getCurrentTenants(HttpServletRequest request) {
        return landlordAfterService.getCurrentTenants(request);
    }

    @GetMapping("/tenant/history")
    public Result getHistoryTenants(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size,
                                    HttpServletRequest request) {
        return landlordAfterService.getHistoryTenants(page, size, request);
    }

    @GetMapping("/tenant/{contractId}")
    public Result getTenantDetail(@PathVariable Long contractId, HttpServletRequest request) {
        return landlordAfterService.getTenantDetail(contractId, request);
    }

    @GetMapping("/issue/list")
    public Result getIssueList(@RequestParam(required = false) Integer status,
                               @RequestParam(required = false) Long houseId,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size,
                               HttpServletRequest request) {
        return landlordAfterService.getIssueList(status, houseId, page, size, request);
    }

    @GetMapping("/issue/stats")
    public Result getIssueStats(HttpServletRequest request) {
        return landlordAfterService.getIssueStats(request);
    }

    @PutMapping("/issue/process/{manageId}")
    public Result processIssue(@PathVariable Long manageId, @RequestBody Map<String, Object> params,
                               HttpServletRequest request) {
        return landlordAfterService.processIssue(manageId, params, request);
    }

    @GetMapping("/checkout/list")
    public Result getCheckoutList(@RequestParam(required = false) Integer status,
                                  @RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size,
                                  HttpServletRequest request) {
        return landlordAfterService.getCheckoutList(status, page, size, request);
    }

    @GetMapping("/checkout/stats")
    public Result getCheckoutStats(HttpServletRequest request) {
        return landlordAfterService.getCheckoutStats(request);
    }

    @PutMapping("/checkout/audit/{manageId}")
    public Result auditCheckout(@PathVariable Long manageId, @RequestBody Map<String, Object> params,
                                HttpServletRequest request) {
        return landlordAfterService.auditCheckout(manageId, params, request);
    }

    @PutMapping("/checkout/handover/{manageId}")
    public Result arrangeHandover(@PathVariable Long manageId, @RequestBody Map<String, Object> params,
                                  HttpServletRequest request) {
        return landlordAfterService.arrangeHandover(manageId, params, request);
    }

    @PutMapping("/checkout/complete/{manageId}")
    public Result completeCheckout(@PathVariable Long manageId, @RequestBody Map<String, Object> params,
                                   HttpServletRequest request) {
        return landlordAfterService.completeCheckout(manageId, params, request);
    }
}
