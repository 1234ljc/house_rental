package com.example.demo.service.comment.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Result;
import com.example.demo.repository.comment.entity.CommentReport;
import com.example.demo.repository.comment.entity.HouseComment;
import com.example.demo.repository.comment.CommentReportMapper;
import com.example.demo.repository.comment.HouseCommentMapper;
import com.example.demo.service.comment.CommentService;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentServiceImpl implements CommentService {

    private final HouseCommentMapper houseCommentMapper;
    private final CommentReportMapper commentReportMapper;
    private final UserMapper userMapper;
    private final HouseMapper houseMapper;

    public CommentServiceImpl(HouseCommentMapper houseCommentMapper,
                              CommentReportMapper commentReportMapper,
                              UserMapper userMapper,
                              HouseMapper houseMapper) {
        this.houseCommentMapper = houseCommentMapper;
        this.commentReportMapper = commentReportMapper;
        this.userMapper = userMapper;
        this.houseMapper = houseMapper;
    }

    @Override
    public Result getComments(Long houseId, Integer page, Integer size, HttpServletRequest request) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }

        Long currentUserId = (Long) request.getAttribute("userId");

        Page<HouseComment> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<HouseComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HouseComment::getHouseId, houseId)
                .isNull(HouseComment::getParentId)
                .eq(HouseComment::getStatus, 0)
                .orderByDesc(HouseComment::getCreateTime);

        Page<HouseComment> result = houseCommentMapper.selectPage(pageParam, wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (HouseComment c : result.getRecords()) {
            Map<String, Object> map = buildCommentMap(c, currentUserId);
            map.put("replies", getRepliesInternal(c.getCommentId(), currentUserId));
            records.add(map);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @Override
    public Result getReplies(Long commentId, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        return Result.success(getRepliesInternal(commentId, currentUserId));
    }

    @Override
    public Result postComment(Long houseId, Map<String, Object> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.failure("请先登录");
        }

        String content = (String) params.get("content");
        if (StrUtil.isBlank(content)) {
            return Result.failure("内容不能为空");
        }
        if (content.length() > 500) {
            return Result.failure("内容不能超过500字");
        }

        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }

        Long parentId = params.get("parentId") != null ? Long.valueOf(params.get("parentId").toString()) : null;
        Long replyToUserId = params.get("replyToUserId") != null ? Long.valueOf(params.get("replyToUserId").toString()) : null;

        if (parentId != null) {
            HouseComment parent = houseCommentMapper.selectById(parentId);
            if (parent == null || parent.getStatus() != 0) {
                return Result.failure("原帖不存在");
            }
        }

        boolean hasRented = houseCommentMapper.checkHasRented(houseId, userId) > 0;

        HouseComment comment = new HouseComment();
        comment.setHouseId(houseId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setContent(content.trim());
        comment.setHasRented(hasRented);
        comment.setLikeCount(0);
        comment.setStatus(0);
        comment.setCreateTime(LocalDateTime.now());

        houseCommentMapper.insert(comment);
        return Result.success(buildCommentMap(comment, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteComment(Long commentId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.failure("请先登录");
        }

        HouseComment comment = houseCommentMapper.selectById(commentId);
        if (comment == null) {
            return Result.failure("帖子不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            return Result.failure("只能删除自己的帖子");
        }

        comment.setStatus(1);
        houseCommentMapper.updateById(comment);

        LambdaQueryWrapper<HouseComment> replyWrapper = new LambdaQueryWrapper<>();
        replyWrapper.eq(HouseComment::getParentId, commentId);
        for (HouseComment r : houseCommentMapper.selectList(replyWrapper)) {
            r.setStatus(1);
            houseCommentMapper.updateById(r);
        }

        return Result.success("删除成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result reportComment(Long commentId, Map<String, String> params, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.failure("请先登录");
        }

        HouseComment comment = houseCommentMapper.selectById(commentId);
        if (comment == null) {
            return Result.failure("帖子不存在");
        }
        if (comment.getUserId().equals(userId)) {
            return Result.failure("不能举报自己的帖子");
        }

        LambdaQueryWrapper<CommentReport> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(CommentReport::getCommentId, commentId).eq(CommentReport::getReporterId, userId);
        if (commentReportMapper.selectCount(checkWrapper) > 0) {
            return Result.failure("您已举报过该帖子");
        }

        String reason = params.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            return Result.failure("请填写举报原因");
        }

        CommentReport report = new CommentReport();
        report.setCommentId(commentId);
        report.setReporterId(userId);
        report.setReason(reason.trim());
        report.setStatus(0);
        report.setCreateTime(LocalDateTime.now());
        commentReportMapper.insert(report);

        return Result.success("举报成功，等待管理员审核");
    }

    private List<Map<String, Object>> getRepliesInternal(Long parentId, Long currentUserId) {
        LambdaQueryWrapper<HouseComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HouseComment::getParentId, parentId)
                .eq(HouseComment::getStatus, 0)
                .orderByAsc(HouseComment::getCreateTime);
        List<HouseComment> replies = houseCommentMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (HouseComment r : replies) {
            Map<String, Object> map = buildCommentMap(r, currentUserId);
            if (r.getReplyToUserId() != null) {
                User replyTo = userMapper.selectById(r.getReplyToUserId());
                if (replyTo != null) {
                    map.put("replyToUsername", replyTo.getRealName() != null ? replyTo.getRealName() : replyTo.getUsername());
                }
            }
            result.add(map);
        }
        return result;
    }

    private Map<String, Object> buildCommentMap(HouseComment c, Long currentUserId) {
        Map<String, Object> map = new HashMap<>();
        map.put("commentId", c.getCommentId());
        map.put("content", c.getContent());
        map.put("hasRented", c.getHasRented());
        map.put("status", c.getStatus());
        map.put("parentId", c.getParentId());
        map.put("createTime", c.getCreateTime());
        map.put("isOwn", currentUserId != null && currentUserId.equals(c.getUserId()));

        User user = userMapper.selectById(c.getUserId());
        if (user != null) {
            map.put("userId", user.getUserId());
            map.put("username", user.getUsername());
            map.put("realName", user.getRealName());
            map.put("avatar", user.getAvatar());
            map.put("userType", user.getUserType());
        }
        return map;
    }
}
