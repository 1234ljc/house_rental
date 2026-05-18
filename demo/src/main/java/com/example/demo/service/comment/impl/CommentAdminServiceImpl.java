package com.example.demo.service.comment.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Result;
import com.example.demo.repository.comment.entity.CommentReport;
import com.example.demo.repository.comment.entity.HouseComment;
import com.example.demo.repository.comment.CommentReportMapper;
import com.example.demo.repository.comment.HouseCommentMapper;
import com.example.demo.service.comment.CommentAdminService;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.UserMapper;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentAdminServiceImpl implements CommentAdminService {

    private final HouseCommentMapper houseCommentMapper;
    private final CommentReportMapper commentReportMapper;
    private final UserMapper userMapper;
    private final HouseMapper houseMapper;

    public CommentAdminServiceImpl(HouseCommentMapper houseCommentMapper,
                                   CommentReportMapper commentReportMapper,
                                   UserMapper userMapper,
                                   HouseMapper houseMapper) {
        this.houseCommentMapper = houseCommentMapper;
        this.commentReportMapper = commentReportMapper;
        this.userMapper = userMapper;
        this.houseMapper = houseMapper;
    }

    @Override
    public Result getReports(Integer status, Integer page, Integer size) {
        Page<CommentReport> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CommentReport> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(CommentReport::getStatus, status);
        }
        wrapper.orderByDesc(CommentReport::getCreateTime);

        Page<CommentReport> result = commentReportMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (CommentReport r : result.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("reportId", r.getReportId());
            map.put("reason", r.getReason());
            map.put("status", r.getStatus());
            map.put("auditRemark", r.getAuditRemark());
            map.put("createTime", r.getCreateTime());
            map.put("auditTime", r.getHandleTime());

            User reporter = userMapper.selectById(r.getReporterId());
            if (reporter != null) {
                map.put("reporterName", reporter.getRealName() != null ? reporter.getRealName() : reporter.getUsername());
                map.put("reporterAvatar", reporter.getAvatar());
            }

            HouseComment comment = houseCommentMapper.selectById(r.getCommentId());
            if (comment != null) {
                map.put("commentId", comment.getCommentId());
                map.put("commentContent", comment.getContent());
                map.put("commentStatus", comment.getStatus());
                map.put("commentTime", comment.getCreateTime());

                User author = userMapper.selectById(comment.getUserId());
                if (author != null) {
                    map.put("authorName", author.getRealName() != null ? author.getRealName() : author.getUsername());
                    map.put("authorAvatar", author.getAvatar());
                }

                House house = houseMapper.selectById(comment.getHouseId());
                if (house != null) {
                    map.put("houseTitle", house.getTitle());
                    map.put("houseId", house.getHouseId());
                }
            }
            records.add(map);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @Override
    public Result getReportStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", commentReportMapper.selectCount(new LambdaQueryWrapper<CommentReport>().eq(CommentReport::getStatus, 0)));
        stats.put("approved", commentReportMapper.selectCount(new LambdaQueryWrapper<CommentReport>().eq(CommentReport::getStatus, 1)));
        stats.put("rejected", commentReportMapper.selectCount(new LambdaQueryWrapper<CommentReport>().eq(CommentReport::getStatus, 2)));
        stats.put("totalComments", houseCommentMapper.selectCount(null));
        return Result.success(stats);
    }

    @Override
    public Result approveReport(Long reportId, Map<String, String> params, HttpServletRequest request) {
        CommentReport report = commentReportMapper.selectById(reportId);
        if (report == null) {
            return Result.failure("举报不存在");
        }
        if (report.getStatus() != 0) {
            return Result.failure("该举报已处理");
        }

        report.setStatus(1);
        report.setAuditRemark(params != null ? params.get("remark") : null);
        report.setHandleTime(LocalDateTime.now());
        commentReportMapper.updateById(report);

        HouseComment comment = houseCommentMapper.selectById(report.getCommentId());
        if (comment != null) {
            comment.setStatus(2);
            houseCommentMapper.updateById(comment);
        }

        return Result.success("已删除该帖子");
    }

    @Override
    public Result rejectReport(Long reportId, Map<String, String> params, HttpServletRequest request) {
        CommentReport report = commentReportMapper.selectById(reportId);
        if (report == null) {
            return Result.failure("举报不存在");
        }
        if (report.getStatus() != 0) {
            return Result.failure("该举报已处理");
        }

        report.setStatus(2);
        report.setAuditRemark(params != null ? params.get("remark") : null);
        report.setHandleTime(LocalDateTime.now());
        commentReportMapper.updateById(report);

        HouseComment comment = houseCommentMapper.selectById(report.getCommentId());
        if (comment != null && comment.getStatus() == 1) {
            comment.setStatus(0);
            houseCommentMapper.updateById(comment);
        }

        return Result.success("已驳回举报");
    }
}
