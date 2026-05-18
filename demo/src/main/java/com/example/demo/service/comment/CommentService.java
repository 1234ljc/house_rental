package com.example.demo.service.comment;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface CommentService{
    Result getComments(Long houseId, Integer page, Integer size, HttpServletRequest request);

    Result getReplies(Long commentId, HttpServletRequest request);

    Result postComment(Long houseId, Map<String, Object> params, HttpServletRequest request);

    Result deleteComment(Long commentId, HttpServletRequest request);

    Result reportComment(Long commentId, Map<String, String> params, HttpServletRequest request);
}
