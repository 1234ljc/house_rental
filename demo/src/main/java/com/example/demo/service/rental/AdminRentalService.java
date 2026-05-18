package com.example.demo.service.rental;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface AdminRentalService{
    Result getContractList(Integer status, String keyword, Integer page, Integer size);

    Result getContractDetail(Long id);

    Result getContractStats();

    Result terminateContract(Long id, Map<String, String> body);
}
