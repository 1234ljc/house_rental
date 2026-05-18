package com.example.demo.entity.vo;

import lombok.Data;

/**
 * 房源视图对象 - 用于返回给前端的数据封装
 * 只包含前端需要的字段
 */
@Data
public class HouseVO {

    /**
     * 房源ID
     */
    private Long houseId;

    /**
     * 房源标题
     */
    private String title;

    /**
     * 地址
     */
    private String address;

    /**
     * 省
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 区
     */
    private String district;

    /**
     * 月租价格
     */
    private Integer rentPrice;

    /**
     * 押金类型：1-押一付一 2-押二付一 3-押一付三
     */
    private String depositType;

    /**
     * 面积
     */
    private Integer area;

    /**
     * 户型：1-一室 2-两室 3-三室 4-四室及以上
     */
    private String houseType;

    /**
     * 朝向：1-南 2-北 3-东 4-西
     */
    private String orientation;

    /**
     * 设施（JSON格式）
     */
    private String facilities;

    /**
     * 图片列表
     */
    private String images;

    /**
     * 付款方式：1-月付 2-季付 3-半年付 4-年付
     */
    private String rentOption;

    /**
     * 收藏数
     */
    private Integer collectCount;

    /**
     * 看房数
     */
    private Integer viewCount;

    /**
     * 房东信息（嵌套VO）
     */
    private LandlordVO landlord;
}
