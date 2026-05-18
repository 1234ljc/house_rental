package com.example.demo.controller.rental;

import com.example.demo.entity.Result;
import com.example.demo.service.rental.LandlordContractService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/landlord/contract")
public class LandlordContractController {

    private final LandlordContractService landlordContractService;

    public LandlordContractController(LandlordContractService landlordContractService) {
        this.landlordContractService = landlordContractService;
    }

    @GetMapping("/my")
    public Result getMyContracts(HttpServletRequest request) {
        return landlordContractService.getMyContracts(request);
    }

    @PostMapping("/upload")
    public Result uploadContract(@RequestParam("file") MultipartFile file,
                                 @RequestParam("name") String name,
                                 HttpServletRequest request) {
        return landlordContractService.uploadContract(file, name, request);
    }

    @DeleteMapping("/{id}")
    public Result deleteContract(@PathVariable String id, HttpServletRequest request) {
        return landlordContractService.deleteContract(id, request);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadContract(@PathVariable String id, HttpServletRequest request) {
        return landlordContractService.downloadContract(id, request);
    }
}
