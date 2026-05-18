package com.example.demo.controller.chat;

import com.example.demo.entity.Result;
import com.example.demo.service.chat.ChatService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

/**
 * 聊天控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/sessions")
    public Result getSessionList(@RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "20") Integer size,
                                 HttpServletRequest request) {
        return chatService.getSessionList(page, size, request);
    }

    @GetMapping("/unread-count")
    public Result getUnreadCount(HttpServletRequest request) {
        return chatService.getUnreadCount(request);
    }

    @PostMapping("/session")
    public Result createOrGetSession(@RequestBody Map<String, Long> params, HttpServletRequest request) {
        return chatService.createOrGetSession(params, request);
    }

    @GetMapping("/session/{sessionId}")
    public Result getSessionDetail(@PathVariable Long sessionId, HttpServletRequest request) {
        return chatService.getSessionDetail(sessionId, request);
    }

    @GetMapping("/messages/{sessionId}")
    public Result getMessages(@PathVariable Long sessionId,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "50") Integer size,
                              HttpServletRequest request) {
        return chatService.getMessages(sessionId, page, size, request);
    }

    @PostMapping("/send")
    public Result sendMessage(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        return chatService.sendMessage(params, request);
    }

    @MessageMapping("/chat.send")
    public void handleWebSocketMessage(@Payload Map<String, Object> payload, Principal principal) {
        chatService.handleWebSocketMessage(payload, principal);
    }

    @PutMapping("/read/{sessionId}")
    public Result markAsRead(@PathVariable Long sessionId, HttpServletRequest request) {
        return chatService.markAsRead(sessionId, request);
    }

    @PostMapping("/upload")
    public Result uploadFile(@RequestParam("file") MultipartFile file,
                             @RequestParam("sessionId") Long sessionId,
                             @RequestParam(value = "type", defaultValue = "image") String type,
                             HttpServletRequest request) {
        return chatService.uploadFile(file, sessionId, type, request);
    }

    @GetMapping("/download/{sessionId}/{fileName}")
    public void downloadFile(@PathVariable Long sessionId,
                             @PathVariable String fileName,
                             @RequestParam(required = false) String name,
                             HttpServletRequest request,
                             HttpServletResponse response) throws IOException {
        chatService.downloadFile(sessionId, fileName, name, request, response);
    }
}
