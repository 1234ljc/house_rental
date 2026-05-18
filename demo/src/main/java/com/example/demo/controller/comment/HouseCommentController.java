package com.example.demo.controller.comment;

import com.example.demo.entity.Result;
import com.example.demo.service.comment.CommentService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/house-comment")
public class HouseCommentController {

    private final CommentService commentService;

    public HouseCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /** 获取顶层帖子列表（带回复） */
    @GetMapping("/{houseId}")
    public Result getComments(@PathVariable Long houseId,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "20") Integer size,
                              HttpServletRequest request) {
        return commentService.getComments(houseId, page, size, request);
    }

    /** 获取某帖子的所有回复 */
    @GetMapping("/replies/{commentId}")
    public Result getRepliesApi(@PathVariable Long commentId, HttpServletRequest request) {
        return commentService.getReplies(commentId, request);
    }

    /** 发表帖子或回复 */
    @PostMapping("/{houseId}")
    public Result postComment(@PathVariable Long houseId,
                              @RequestBody Map<String, Object> params,
                              HttpServletRequest request) {
        return commentService.postComment(houseId, params, request);
    }

    /** 删除自己的帖子（软删除，同时删除其下所有回复） */
    @DeleteMapping("/{commentId}")
    public Result deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        return commentService.deleteComment(commentId, request);
    }

    /** 举报帖子 */
    @PostMapping("/report/{commentId}")
    public Result reportComment(@PathVariable Long commentId,
                                @RequestBody Map<String, String> params,
                                HttpServletRequest request) {
        return commentService.reportComment(commentId, params, request);
    }
}
