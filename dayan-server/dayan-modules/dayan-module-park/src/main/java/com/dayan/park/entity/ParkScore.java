package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 park_score 对应实体：机构评分（独立表，避免写热点）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_score")
public class ParkScore extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 机构编码 */
    private String parkCode;

    /** 总评分 */
    private Integer scoreTotal;

    /** 环境评分 */
    private Integer scoreEnvironment;

    /** 文娱评分 */
    private Integer scoreRecreation;

    /** 医养护理评分 */
    private Integer scoreNursing;

    /** 餐食精细评分 */
    private Integer scoreFood;

    /** 服务品质评分 */
    private Integer scoreService;

    /** 价格评分 */
    private Integer scorePrice;

    /** 评分描述 */
    private String scoreDescription;

    /** 乐观锁版本 */
    @Version
    private Long version;
}
