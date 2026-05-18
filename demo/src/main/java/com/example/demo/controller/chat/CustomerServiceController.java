package com.example.demo.controller.chat;

import com.example.demo.entity.Result;
import com.example.demo.service.chat.CustomerServiceService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 客服控制器 - 用户端（租客/房东联系客服）
 */
@RestController
@RequestMapping("/api/customer-service")
public class CustomerServiceController {

    private final CustomerServiceService customerServiceService;

    public CustomerServiceController(CustomerServiceService customerServiceService) {
        this.customerServiceService = customerServiceService;
    }

    @PostMapping("/session")
    public Result createOrGetSession(HttpServletRequest request) {
        return customerServiceService.createOrGetSession(request);
    }

    @GetMapping("/session/{sessionId}")
    public Result getSessionDetail(@PathVariable Long sessionId, HttpServletRequest request) {
        return customerServiceService.getSessionDetail(sessionId, request);
    }

    @GetMapping("/messages/{sessionId}")
    public Result getMessages(@PathVariable Long sessionId,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "50") Integer size,
                              HttpServletRequest request) {
        return customerServiceService.getMessages(sessionId, page, size, request);
    }

    @PostMapping("/send")
    public Result sendMessage(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        return customerServiceService.sendMessage(params, request);
    }

    @GetMapping("/unread-count")
    public Result getUnreadCount(HttpServletRequest request) {
        return customerServiceService.getUnreadCount(request);
    }

    @PostMapping("/close/{sessionId}")
    public Result closeSession(@PathVariable Long sessionId, HttpServletRequest request) {
        return customerServiceService.closeSession(sessionId, request);
    }

    @GetMapping("/queue-position")
    public Result getQueuePosition(HttpServletRequest request) {
        return customerServiceService.getQueuePosition(request);
    }
}
