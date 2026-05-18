package com.example.demo.repository.rental;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.repository.rental.entity.RentalContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RentalContractMapper extends BaseMapper<RentalContract> {

    /**
     * 统计房东待确认合同数（待租客确认）
     */
    @Select("SELECT COUNT(*) FROM rental_contract " +
            "WHERE landlord_id = #{landlordId} AND status = 1")
    Long countPendingSignByLandlordId(@Param("landlordId") Long landlordId);
    
    /**
     * 统计已确认的合同数
     */
    @Select("SELECT COUNT(*) FROM rental_contract " +
            "WHERE landlord_id = #{landlordId} AND status = 2")
    Long countWaitingLandlordSign(@Param("landlordId") Long landlordId);

    /**
     * 按状态分组统计合同数量（房东）
     */
    @Select("SELECT status, COUNT(*) as cnt FROM rental_contract WHERE landlord_id = #{landlordId} GROUP BY status")
    java.util.List<java.util.Map<String, Object>> countByStatusForLandlord(@Param("landlordId") Long landlordId);

    /**
     * 按状态分组统计合同数量（租客）
     */
    @Select("SELECT status, COUNT(*) as cnt FROM rental_contract WHERE tenant_id = #{tenantId} GROUP BY status")
    java.util.List<java.util.Map<String, Object>> countByStatusForTenant(@Param("tenantId") Long tenantId);

    /**
     * 近30天合同创建趋势（一条SQL）
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as count FROM rental_contract " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(create_time) ORDER BY date")
    java.util.List<java.util.Map<String, Object>> getLast30DaysTrend();

    /**
     * 统计房东待处理的续租申请数
     */
    @Select("SELECT COUNT(*) FROM rental_contract " +
            "WHERE landlord_id = #{landlordId} AND renewal_status = 1")
    Long countPendingRenewalByLandlordId(@Param("landlordId") Long landlordId);
}
