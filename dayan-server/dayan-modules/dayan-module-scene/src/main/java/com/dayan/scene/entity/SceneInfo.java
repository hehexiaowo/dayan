package com.dayan.scene.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
/**
 * 表 scene_info 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scene_info")
public class SceneInfo extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 场景编码 */
    private String sceneCode;

    /** 场景名称 */
    private String sceneName;

    /** 场景类型 */
    private Integer sceneType;

    /** 关联养老机构编码 */
    private String parkCode;

    /** 省份编码 */
    private String provinceCode;

    /** 城市编码 */
    private String cityCode;

    /** 区划编码 */
    private String districtCode;

    /** 活动地址 */
    private String address;

    /** 场景详细描述 */
    private String sceneDescription;

    /** 封面图URL */
    private String coverImage;

    /** 场景图片URL列表 */
    private String imageUrls;

    /** 宣传视频URL */
    private String videoUrl;

    /** 最大容纳人数 */
    private Integer capacity;

    /** 预计时长(小时) */
    private BigDecimal durationHours;

    /** 目标人群描述 */
    private String targetAudience;

    /** 场景亮点 */
    private String highlight;

    /** 注意事项 */
    private String notice;

    /** 最低成团人数 */
    private Integer minPerson;

    /** 最大参与人数 */
    private Integer maxPerson;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 售价 */
    private BigDecimal salePrice;

    /** 价格单位 */
    private String priceUnit;

    /** 是否免费 */
    private Integer isFree;

    /** 排序号 */
    private Integer sortOrder;

    /** 浏览次数 */
    private Integer viewCount;

    /** 预约次数 */
    private Integer bookCount;

    /** 场景状态 */
    private Integer sceneStatus;

    /** 审核状态 */
    private Integer auditStatus;

    /** 备注 */
    private String remark;
}
