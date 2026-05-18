package com.example.demo.controller.comment;

import com.example.demo.entity.Result;
import com.example.demo.service.comment.CommentAdminService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/comment")
public class AdminCommentController {

    private final CommentAdminService commentAdminService;

    public AdminCommentController(CommentAdminService commentAdminService) {
        this.commentAdminService = commentAdminService;
    }

    @GetMapping("/reports")
    public Result getReports(@RequestParam(required = false) Integer status,
                             @RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer size) {
        return commentAdminService.getReports(status, page, size);
    }

    @GetMapping("/reports/stats")
    public Result getReportStats() {
        return commentAdminService.getReportStats();
    }

    @PostMapping("/reports/approve/{reportId}")
    public Result approveReport(@PathVariable Long reportId,
                                @RequestBody(required = false) Map<String, String> params,
                                HttpServletRequest request) {
        return commentAdminService.approveReport(reportId, params, request);
    }

    @PostMapping("/reports/reject/{reportId}")
    public Result rejectReport(@PathVariable Long reportId,
                               @RequestBody(required = false) Map<String, String> params,
                               HttpServletRequest request) {
        return commentAdminService.rejectReport(reportId, params, request);
    }
}
