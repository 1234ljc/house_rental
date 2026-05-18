package com.example.demo.repository.rental;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.repository.rental.entity.RentalManage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RentalManageMapper extends BaseMapper<RentalManage> {

    /**
     * 统计房东待处理问题数
     */
    @Select("SELECT COUNT(*) FROM rental_manage rm " +
            "INNER JOIN rental_contract rc ON rm.contract_id = rc.contract_id " +
            "WHERE rc.landlord_id = #{landlordId} AND rm.status = 0")
    Long countPendingByLandlordId(@Param("landlordId") Long landlordId);

    /**
     * 统计租客待处理问题数
     */
    @Select("SELECT COUNT(*) FROM rental_manage rm " +
            "WHERE rm.user_id = #{tenantId} AND rm.status IN (0, 1)")
    Long countPendingByTenantId(@Param("tenantId") Long tenantId);
}
