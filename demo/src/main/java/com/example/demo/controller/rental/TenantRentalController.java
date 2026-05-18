package com.example.demo.controller.rental;

import com.example.demo.entity.Result;
import com.example.demo.service.rental.TenantRentalService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tenant/rental")
public class TenantRentalController {

    private final TenantRentalService tenantRentalService;

    public TenantRentalController(TenantRentalService tenantRentalService) {
        this.tenantRentalService = tenantRentalService;
    }

    @GetMapping("/contract/stats")
    public Result getContractStats(HttpServletRequest request) {
        return tenantRentalService.getContractStats(request);
    }

    @GetMapping("/contract/list")
    public Result getContractList(@RequestParam(required = false) Integer status,
                                  @RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size,
                                  HttpServletRequest request) {
        return tenantRentalService.getContractList(status, page, size, request);
    }

    @GetMapping("/contract/{contractId}")
    public Result getContractDetail(@PathVariable Long contractId, HttpServletRequest request) {
        return tenantRentalService.getContractDetail(contractId, request);
    }

    @PostMapping("/contract/verify-identity")
    public Result verifyIdentity(@RequestBody Map<String, String> params, HttpServletRequest request) {
        return tenantRentalService.verifyIdentity(params, request);
    }

    @PutMapping("/contract/sign/{contractId}")
    public Result confirmContract(@PathVariable Long contractId,
                                  @RequestBody(required = false) Map<String, String> params,
                                  HttpServletRequest request) {
        return tenantRentalService.confirmContract(contractId, params, request);
    }

    @GetMapping("/contract/download/{contractId}")
    public ResponseEntity<Resource> downloadContract(@PathVariable Long contractId, HttpServletRequest request) {
        return tenantRentalService.downloadContract(contractId, request);
    }

    @PostMapping("/contract/renewal/{contractId}")
    public Result applyRenewal(@PathVariable Long contractId, HttpServletRequest request) {
        return tenantRentalService.applyRenewal(contractId, request);
    }
}
