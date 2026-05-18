package com.example.demo.service.rental;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface LandlordContractService{
    Result getMyContracts(HttpServletRequest request);

    Result uploadContract(MultipartFile file, String name, HttpServletRequest request);

    Result deleteContract(String id, HttpServletRequest request);

    ResponseEntity<Resource> downloadContract(String id, HttpServletRequest request);
}
