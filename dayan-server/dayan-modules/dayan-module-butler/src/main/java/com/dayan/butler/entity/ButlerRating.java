package com.dayan.butler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 butler_rating 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("butler_rating")
public class ButlerRating extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 管家编码 */
    private String butlerCode;

    /** 客户编码 */
    private String clientCode;

    /** 关联服务记录编码 */
    private String serviceRecordCode;

    /** 评分 */
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 状态 */
    private Integer status;
}
