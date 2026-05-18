package com.example.demo.repository.rental;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.repository.rental.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    /**
     * 统计今日订单数量
     */
    @Select("SELECT COUNT(*) FROM payment_order WHERE DATE(create_time) = CURDATE()")
    Long countTodayOrders();

    /**
     * 统计今日订单金额
     */
    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM payment_order " +
            "WHERE DATE(payment_time) = CURDATE() AND payment_status = 1")
    BigDecimal sumTodayAmount();

    /**
     * 统计平台总交易额
     */
    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM payment_order WHERE payment_status = 1")
    BigDecimal sumTotalAmount();

    /**
     * 近7天交易额趋势
     */
    @Select("SELECT DATE(payment_time) as date, COALESCE(SUM(pay_amount), 0) as amount " +
            "FROM payment_order WHERE payment_status = 1 " +
            "AND payment_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY DATE(payment_time) ORDER BY date")
    List<Map<String, Object>> getLast7DaysAmountTrend();

    /**
     * 近30天每日收入趋势（管理端，一条SQL）
     */
    @Select("SELECT DATE(payment_time) as date, COALESCE(SUM(pay_amount), 0) as amount " +
            "FROM payment_order WHERE payment_status = 1 " +
            "AND payment_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(payment_time) ORDER BY date")
    List<Map<String, Object>> getLast30DaysAmountTrend();

    /**
     * 房东近12个月每月收入趋势（一条SQL）
     */
    @Select("SELECT DATE_FORMAT(po.payment_time, '%Y-%m') as month, COALESCE(SUM(po.pay_amount), 0) as income " +
            "FROM payment_order po " +
            "INNER JOIN rental_contract rc ON po.contract_id = rc.contract_id " +
            "WHERE rc.landlord_id = #{landlordId} AND po.payment_status = 1 " +
            "AND po.order_type != 2 " +
            "AND po.payment_time >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
            "GROUP BY DATE_FORMAT(po.payment_time, '%Y-%m') ORDER BY month")
    List<Map<String, Object>> getLast12MonthsIncomeByLandlordId(@Param("landlordId") Long landlordId);

    /**
     * 统计房东待收租金数
     */
    @Select("SELECT COUNT(*) FROM payment_order po " +
            "INNER JOIN rental_contract rc ON po.contract_id = rc.contract_id " +
            "WHERE rc.landlord_id = #{landlordId} AND po.payment_status = 0")
    Long countPendingByLandlordId(@Param("landlordId") Long landlordId);

    /**
     * 统计房东逾期租金数
     */
    @Select("SELECT COUNT(*) FROM payment_order po " +
            "INNER JOIN rental_contract rc ON po.contract_id = rc.contract_id " +
            "WHERE rc.landlord_id = #{landlordId} AND po.payment_status = 0 " +
            "AND po.create_time < DATE_SUB(CURDATE(), INTERVAL 3 DAY)")
    Long countOverdueByLandlordId(@Param("landlordId") Long landlordId);

    /**
     * 统计房东本月已收租金
     */
    @Select("SELECT COALESCE(SUM(po.pay_amount), 0) FROM payment_order po " +
            "INNER JOIN rental_contract rc ON po.contract_id = rc.contract_id " +
            "WHERE rc.landlord_id = #{landlordId} AND po.payment_status = 1 " +
            "AND YEAR(po.payment_time) = YEAR(CURDATE()) AND MONTH(po.payment_time) = MONTH(CURDATE())")
    BigDecimal sumMonthlyReceivedByLandlordId(@Param("landlordId") Long landlordId);

    /**
     * 统计房东本月预计收入
     */
    @Select("SELECT COALESCE(SUM(po.total_amount), 0) FROM payment_order po " +
            "INNER JOIN rental_contract rc ON po.contract_id = rc.contract_id " +
            "WHERE rc.landlord_id = #{landlordId} " +
            "AND YEAR(po.create_time) = YEAR(CURDATE()) AND MONTH(po.create_time) = MONTH(CURDATE())")
    BigDecimal sumMonthlyExpectedByLandlordId(@Param("landlordId") Long landlordId);

    /**
     * 房东近30天收入趋势
     */
    @Select("SELECT DATE(po.payment_time) as date, COALESCE(SUM(po.pay_amount), 0) as amount " +
            "FROM payment_order po " +
            "INNER JOIN rental_contract rc ON po.contract_id = rc.contract_id " +
            "WHERE rc.landlord_id = #{landlordId} AND po.payment_status = 1 " +
            "AND po.payment_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(po.payment_time) ORDER BY date")
    List<Map<String, Object>> getLast30DaysIncomeByLandlordId(@Param("landlordId") Long landlordId);

    /**
     * 房东房源收入排行TOP5
     */
    @Select("SELECT h.title as houseName, COALESCE(SUM(po.pay_amount), 0) as income " +
            "FROM payment_order po " +
            "INNER JOIN rental_contract rc ON po.contract_id = rc.contract_id " +
            "INNER JOIN house h ON rc.house_id = h.house_id " +
            "WHERE rc.landlord_id = #{landlordId} AND po.payment_status = 1 " +
            "AND YEAR(po.payment_time) = YEAR(CURDATE()) AND MONTH(po.payment_time) = MONTH(CURDATE()) " +
            "GROUP BY h.house_id, h.title ORDER BY income DESC LIMIT 5")
    List<Map<String, Object>> getHouseIncomeRankByLandlordId(@Param("landlordId") Long landlordId);
}
