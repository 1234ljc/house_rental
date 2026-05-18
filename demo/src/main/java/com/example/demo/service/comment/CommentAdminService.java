package com.example.demo.service.comment;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface CommentAdminService{
    Result getReports(Integer status, Integer page, Integer size);

    Result getReportStats();

    Result approveReport(Long reportId, Map<String, String> params, HttpServletRequest request);

    Result rejectReport(Long reportId, Map<String, String> params, HttpServletRequest request);
}
