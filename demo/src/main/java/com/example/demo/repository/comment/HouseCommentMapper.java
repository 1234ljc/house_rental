package com.example.demo.repository.comment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.repository.comment.entity.HouseComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HouseCommentMapper extends BaseMapper<HouseComment> {

    @Select("SELECT COUNT(*) FROM rental_contract WHERE house_id = #{houseId} AND tenant_id = #{userId} AND status IN (2, 3)")
    int checkHasRented(@Param("houseId") Long houseId, @Param("userId") Long userId);
}
