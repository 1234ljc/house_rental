package com.example.demo.repository.house;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.repository.house.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
