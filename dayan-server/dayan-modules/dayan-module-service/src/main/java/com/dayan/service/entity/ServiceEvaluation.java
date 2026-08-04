package com.dayan.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 service_evaluation 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_evaluation")
public class ServiceEvaluation extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 服务会话编码 */
    private String sessionCode;

    /** 客户编码 */
    private String clientCode;

    /** 管家编码 */
    private String butlerCode;

    /** 关联机构编码 */
    private String parkCode;

    /** 服务态度评分 */
    private Integer attitudeRating;

    /** 专业度评分 */
    private Integer professionalRating;

    /** 响应速度评分 */
    private Integer responsivenessRating;

    /** 满意度评分 */
    private Integer satisfactionRating;

    /** 评价内容 */
    private String content;

    /** 评价图片 */
    private String imageUrls;

    /** 是否匿名 */
    private Integer isAnonymous;

    /** 回复内容 */
    private String replyContent;

    /** 回复时间 */
    private LocalDateTime replyTime;

    /** 回复人编码 */
    private String replyByCode;

    /** 状态 */
    private Integer status;
}
