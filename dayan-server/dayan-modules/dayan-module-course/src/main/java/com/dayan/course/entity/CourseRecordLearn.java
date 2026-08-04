package com.dayan.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * 表 course_record_learn 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_record_learn")
public class CourseRecordLearn extends BaseEntity {

    /** 主键（雪花ID，分片表） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 课程编码 */
    private String courseCode;

    /** 学员客户编码 */
    private String clientCode;

    /** 学员代理人编码 */
    private String agentCode;

    /** 学员姓名 */
    private String learnerName;

    /** 学员手机号 */
    private String learnerPhone;

    /** 报名时间 */
    private LocalDateTime enrollTime;

    /** 关联订单编码 */
    private String orderCode;

    /** 当前学到第几课 */
    private Integer currentLesson;

    /** 总课时 */
    private Integer totalLesson;

    /** 学习进度(%) */
    private BigDecimal learnProgress;

    /** 累计学习时长(分钟) */
    private Integer totalLearnTime;

    /** 最近学习时间 */
    private LocalDateTime lastLearnTime;

    /** 是否完成 */
    private Integer isCompleted;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 结业证书URL */
    private String certificateUrl;

    /** 课程评分 */
    private Integer rating;

    /** 评价内容 */
    private String ratingContent;

    /** 状态 */
    private Integer status;
}
