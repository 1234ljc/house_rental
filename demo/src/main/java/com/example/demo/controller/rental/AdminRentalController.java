package com.example.demo.controller.rental;

import com.example.demo.entity.Result;
import com.example.demo.service.rental.AdminRentalService;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/rental")
public class AdminRentalController {

    private final AdminRentalService adminRentalService;

    public AdminRentalController(AdminRentalService adminRentalService) {
        this.adminRentalService = adminRentalService;
    }

    @GetMapping("/contract/list")
    public Result getContractList(@RequestParam(defaultValue = "-1") Integer status,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size) {
        return adminRentalService.getContractList(status, keyword, page, size);
    }

    @GetMapping("/contract/{id}")
    public Result getContractDetail(@PathVariable Long id) {
        return adminRentalService.getContractDetail(id);
    }

    @GetMapping("/contract/stats")
    public Result getContractStats() {
        return adminRentalService.getContractStats();
    }

    @PutMapping("/contract/{id}/terminate")
    public Result terminateContract(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return adminRentalService.terminateContract(id, body);
    }
}
