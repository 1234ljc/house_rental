package com.example.demo.controller.chat;

import com.example.demo.entity.Result;
import com.example.demo.service.chat.AdminCustomerServiceService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员客服控制器 - 客服工作台
 */
@RestController
@RequestMapping("/api/admin/customer-service")
public class AdminCustomerServiceController {

    private final AdminCustomerServiceService adminCustomerServiceService;

    public AdminCustomerServiceController(AdminCustomerServiceService adminCustomerServiceService) {
        this.adminCustomerServiceService = adminCustomerServiceService;
    }

    @GetMapping("/sessions")
    public Result getSessionList(@RequestParam(defaultValue = "0") Integer status,
                                 @RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "20") Integer size,
                                 HttpServletRequest request) {
        return adminCustomerServiceService.getSessionList(status, page, size, request);
    }

    @GetMapping("/statistics")
    public Result getStatistics(HttpServletRequest request) {
        return adminCustomerServiceService.getStatistics(request);
    }

    @PostMapping("/accept/{sessionId}")
    public Result acceptSession(@PathVariable Long sessionId, HttpServletRequest request) {
        return adminCustomerServiceService.acceptSession(sessionId, request);
    }

    @GetMapping("/messages/{sessionId}")
    public Result getMessages(@PathVariable Long sessionId,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "50") Integer size,
                              HttpServletRequest request) {
        return adminCustomerServiceService.getMessages(sessionId, page, size, request);
    }

    @PostMapping("/send")
    public Result sendMessage(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        return adminCustomerServiceService.sendMessage(params, request);
    }

    @PostMapping("/close/{sessionId}")
    public Result closeSession(@PathVariable Long sessionId, HttpServletRequest request) {
        return adminCustomerServiceService.closeSession(sessionId, request);
    }

    @GetMapping("/unread-count")
    public Result getUnreadCount(HttpServletRequest request) {
        return adminCustomerServiceService.getUnreadCount(request);
    }
}
