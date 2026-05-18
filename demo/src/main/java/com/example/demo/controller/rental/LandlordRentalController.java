package com.example.demo.controller.rental;

import com.example.demo.entity.Result;
import com.example.demo.service.rental.LandlordRentalService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/landlord/rental")
public class LandlordRentalController {

    private final LandlordRentalService landlordRentalService;

    public LandlordRentalController(LandlordRentalService landlordRentalService) {
        this.landlordRentalService = landlordRentalService;
    }

    @GetMapping("/contract/stats")
    public Result getContractStats(HttpServletRequest request) {
        return landlordRentalService.getContractStats(request);
    }

    @GetMapping("/contract/list")
    public Result getContractList(@RequestParam(required = false) Integer status,
                                  @RequestParam(required = false) Long houseId,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size,
                                  HttpServletRequest request) {
        return landlordRentalService.getContractList(status, houseId, keyword, page, size, request);
    }

    @GetMapping("/contract/{contractId}")
    public Result getContractDetail(@PathVariable Long contractId, HttpServletRequest request) {
        return landlordRentalService.getContractDetail(contractId, request);
    }

    @PostMapping("/contract/create-direct")
    public Result createContractDirect(@RequestParam("file") MultipartFile file,
                                       @RequestParam("houseId") Long houseId,
                                       @RequestParam("tenantId") Long tenantId,
                                       @RequestParam("rentStartDate") String rentStartDateStr,
                                       @RequestParam("rentMonths") Integer rentMonths,
                                       @RequestParam(value = "monthlyRent", required = false) String monthlyRentStr,
                                       @RequestParam(value = "depositAmount", required = false) String depositAmountStr,
                                       @RequestParam(value = "paymentDay", defaultValue = "1") Integer paymentDay,
                                       HttpServletRequest request) {
        return landlordRentalService.createContractDirect(file, houseId, tenantId, rentStartDateStr, rentMonths, monthlyRentStr, depositAmountStr, paymentDay, request);
    }

    @GetMapping("/tenant/search")
    public Result searchTenant(@RequestParam String keyword, HttpServletRequest request) {
        return landlordRentalService.searchTenant(keyword, request);
    }

    @GetMapping("/chat-tenants")
    public Result getChatTenants(HttpServletRequest request) {
        return landlordRentalService.getChatTenants(request);
    }

    @PutMapping("/contract/update/{contractId}")
    public Result updateContract(@PathVariable Long contractId, @RequestBody Map<String, Object> params,
                                 HttpServletRequest request) {
        return landlordRentalService.updateContract(contractId, params, request);
    }

    @PostMapping("/contract/reupload/{contractId}")
    public Result reuploadContract(@PathVariable Long contractId,
                                   @RequestParam("file") MultipartFile file,
                                   HttpServletRequest request) {
        return landlordRentalService.reuploadContract(contractId, file, request);
    }

    @GetMapping("/contract/download/{contractId}")
    public ResponseEntity<Resource> downloadContract(@PathVariable Long contractId, HttpServletRequest request) {
        return landlordRentalService.downloadContract(contractId, request);
    }

    @PutMapping("/contract/send/{contractId}")
    public Result sendContract(@PathVariable Long contractId, HttpServletRequest request) {
        return landlordRentalService.sendContract(contractId, request);
    }

    @PostMapping("/contract/verify-identity")
    public Result verifyIdentity(@RequestBody Map<String, String> params, HttpServletRequest request) {
        return landlordRentalService.verifyIdentity(params, request);
    }

    @PostMapping("/contract/renewal/approve/{contractId}")
    public Result approveRenewal(@PathVariable Long contractId,
                                 @RequestParam("file") MultipartFile file,
                                 @RequestParam("rentStartDate") String rentStartDateStr,
                                 @RequestParam("rentMonths") Integer rentMonths,
                                 @RequestParam(value = "monthlyRent", required = false) String monthlyRentStr,
                                 @RequestParam(value = "depositAmount", required = false) String depositAmountStr,
                                 @RequestParam(value = "paymentDay", defaultValue = "1") Integer paymentDay,
                                 HttpServletRequest request) {
        return landlordRentalService.approveRenewal(contractId, file, rentStartDateStr, rentMonths, monthlyRentStr, depositAmountStr, paymentDay, request);
    }

    @PutMapping("/contract/renewal/reject/{contractId}")
    public Result rejectRenewal(@PathVariable Long contractId,
                                @RequestBody(required = false) Map<String, String> params,
                                HttpServletRequest request) {
        return landlordRentalService.rejectRenewal(contractId, params, request);
    }

    @PutMapping("/contract/sign/{contractId}")
    public Result signContract(@PathVariable Long contractId, 
                               @RequestBody(required = false) Map<String, String> params,
                               HttpServletRequest request) {
        return landlordRentalService.signContract(contractId, params, request);
    }
}
