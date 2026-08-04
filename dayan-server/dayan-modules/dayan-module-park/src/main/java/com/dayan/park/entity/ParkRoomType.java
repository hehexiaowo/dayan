package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
/**
 * 表 park_room_type 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_room_type")
public class ParkRoomType extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 机构编码 */
    private String parkCode;

    /** 房间类型编码 */
    private String roomTypeCode;

    /** 房间类型名称 */
    private String roomTypeName;

    /** 居住类型 */
    private Integer stayType;

    /** 楼栋名称 */
    private String buildingName;

    /** 所在楼层 */
    private String floor;

    /** 房间类别 */
    private Integer roomCategory;

    /** 房间面积 */
    private BigDecimal area;

    /** 朝向 */
    private String orientation;

    /** 床位数 */
    private Integer bedCount;

    /** 该类型房间总数 */
    private Integer totalRooms;

    /** 可入住数 */
    private Integer availableRooms;

    /** 独立卫生间 */
    private Integer hasBathroom;

    /** 独立厨房 */
    private Integer hasKitchen;

    /** 有阳台 */
    private Integer hasBalcony;

    /** 有电视 */
    private Integer hasTv;

    /** 有空调 */
    private Integer hasAircon;

    /** 有冰箱 */
    private Integer hasFridge;

    /** 有洗衣机 */
    private Integer hasWasher;

    /** 有WiFi */
    private Integer hasWifi;

    /** 有紧急呼叫 */
    private Integer hasEmergency;

    /** 有监控 */
    private Integer hasMonitor;

    /** 配套设施详情 */
    private String facilities;

    /** 房间详细说明 */
    private String description;

    /** 封面图URL */
    private String coverImage;

    /** 房间图片URL列表 */
    private String images;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;

    /** 户型设计描述 */
    private String designDescription;

    /** 户型图URL */
    private String designImage;

    /** 其他户型图片 */
    private String additionalImages;
}
