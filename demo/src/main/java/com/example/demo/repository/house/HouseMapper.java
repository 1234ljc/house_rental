package com.example.demo.repository.house;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.repository.house.entity.House;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface HouseMapper extends BaseMapper<House> {

    /**
     * 按状态统计房源数量
     */
    @Select("SELECT status, COUNT(*) as count FROM house GROUP BY status")
    List<Map<String, Object>> countByStatus();

    /**
     * 统计今日新增房源
     */
    @Select("SELECT COUNT(*) FROM house WHERE DATE(create_time) = CURDATE()")
    Long countTodayNew();

    /**
     * 热门城市排行TOP10
     */
    @Select("SELECT city, COUNT(*) as count FROM house WHERE status = 1 GROUP BY city ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> getHotCities();

    /**
     * 近7天房源增长趋势
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as count FROM house " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> getLast7DaysTrend();

    /**
     * 近30天房源发布趋势（一条SQL）
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as count FROM house " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> getLast30DaysTrend();

    /**
     * 统计待审核房源数量
     */
    @Select("SELECT COUNT(*) FROM house WHERE status = 0")
    Long countPendingAudit();

    /**
     * 统计房东房源总数
     */
    @Select("SELECT COUNT(*) FROM house WHERE landlord_id = #{landlordId}")
    Long countByLandlordId(@Param("landlordId") Long landlordId);

    /**
     * 统计房东指定状态房源数
     */
    @Select("SELECT COUNT(*) FROM house WHERE landlord_id = #{landlordId} AND status = #{status}")
    Long countByLandlordIdAndStatus(@Param("landlordId") Long landlordId, @Param("status") Integer status);

    /**
     * 按状态分组统计房东的房源数量（一条SQL替代多次查询）
     */
    @Select("SELECT status, COUNT(*) as cnt FROM house WHERE landlord_id = #{landlordId} GROUP BY status")
    List<Map<String, Object>> countByStatusForLandlord(@Param("landlordId") Long landlordId);

    /**
     * 获取热门房源（按浏览量排序，支持城市过滤）
     */
    @Select("<script>" +
            "SELECT house_id as houseId, title, address, city, rent_price as rentPrice, " +
            "area, house_type as houseType, images, view_count as viewCount, collect_count as collectCount " +
            "FROM house WHERE status = 1 " +
            "<if test='city != null and city != \"\"'>" +
            "AND city LIKE CONCAT('%',#{city},'%') " +
            "</if>" +
            "ORDER BY view_count DESC, collect_count DESC LIMIT #{limit}" +
            "</script>")
    List<Map<String, Object>> selectHotHouses(@Param("city") String city, @Param("limit") int limit);

    /**
     * 获取低价房源（按价格排序，支持城市过滤）
     */
    @Select("<script>" +
            "SELECT house_id as houseId, title, address, city, rent_price as rentPrice, " +
            "area, house_type as houseType, images, view_count as viewCount, collect_count as collectCount " +
            "FROM house WHERE status = 1 " +
            "<if test='city != null and city != \"\"'>" +
            "AND city LIKE CONCAT('%',#{city},'%') " +
            "</if>" +
            "ORDER BY rent_price ASC LIMIT #{limit}" +
            "</script>")
    List<Map<String, Object>> selectCheapHouses(@Param("city") String city, @Param("limit") int limit);

    /**
     * 获取推荐房源（最新发布，支持城市过滤）
     */
    @Select("<script>" +
            "SELECT house_id as houseId, title, address, city, rent_price as rentPrice, " +
            "area, house_type as houseType, images, view_count as viewCount, collect_count as collectCount " +
            "FROM house WHERE status = 1 " +
            "<if test='city != null and city != \"\"'>" +
            "AND city LIKE CONCAT('%',#{city},'%') " +
            "</if>" +
            "ORDER BY create_time DESC LIMIT #{limit}" +
            "</script>")
    List<Map<String, Object>> selectRecommendHouses(@Param("city") String city, @Param("limit") int limit);

    /**
     * 搜索房源
     */
    @Select("<script>" +
            "SELECT house_id as houseId, title, address, city, rent_price as rentPrice, " +
            "area, house_type as houseType, images, view_count as viewCount, collect_count as collectCount " +
            "FROM house WHERE status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (title LIKE CONCAT('%',#{keyword},'%') OR address LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "<if test='city != null and city != \"\"'>" +
            "AND city = #{city} " +
            "</if>" +
            "<if test='minPrice != null'>" +
            "AND rent_price >= #{minPrice} " +
            "</if>" +
            "<if test='maxPrice != null'>" +
            "AND rent_price &lt;= #{maxPrice} " +
            "</if>" +
            "ORDER BY create_time DESC LIMIT 20" +
            "</script>")
    List<Map<String, Object>> searchHouses(@Param("keyword") String keyword,
                                           @Param("city") String city,
                                           @Param("minPrice") Integer minPrice,
                                           @Param("maxPrice") Integer maxPrice);
}
