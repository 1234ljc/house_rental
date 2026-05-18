package com.example.demo.controller.rental;

import com.example.demo.entity.Result;
import com.example.demo.service.rental.LandlordContractAlertService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/landlord/contract-alert")
@RequiredArgsConstructor
public class LandlordContractAlertController {

    private final LandlordContractAlertService landlordContractAlertService;

    @GetMapping("/stats")
    public Result getStats(HttpServletRequest request) {
        return landlordContractAlertService.getStats(request);
    }

    @GetMapping("/list")
    public Result getList(HttpServletRequest request,
                          @RequestParam(required = false) Integer alertType,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size) {
        return landlordContractAlertService.getList(request, alertType, keyword, page, size);
    }

    @GetMapping("/calendar")
    public Result getCalendar(HttpServletRequest request,
                              @RequestParam(required = false) Integer year,
                              @RequestParam(required = false) Integer month) {
        return landlordContractAlertService.getCalendar(request, year, month);
    }

    @GetMapping("/trend")
    public Result getTrend(HttpServletRequest request) {
        return landlordContractAlertService.getTrend(request);
    }

    @PostMapping("/notify/{contractId}")
    public Result sendNotify(HttpServletRequest request, @PathVariable Long contractId) {
        return landlordContractAlertService.sendNotify(request, contractId);
    }
}
