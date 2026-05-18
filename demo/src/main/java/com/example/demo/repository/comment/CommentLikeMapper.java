package com.example.demo.repository.comment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.repository.comment.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    @Select("SELECT COUNT(*) FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int checkLiked(@Param("commentId") Long commentId, @Param("userId") Long userId);
}
