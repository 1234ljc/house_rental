package com.example.demo.repository.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.repository.user.entity.RealnameAuth;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RealnameAuthMapper extends BaseMapper<RealnameAuth> {

    /**
     * 统计待审核实名认证数量
     */
    @Select("SELECT COUNT(*) FROM realname_auth WHERE auth_status = 0")
    Long countPendingAudit();
}
