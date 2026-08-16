package com.dayan.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 学习中心内容（learning_content）。
 *
 * <p>全局表（无 channel_code），所有代理人可见。
 * 分类：1=视频课程 2=图文课程 3=雁鸣中国。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("learning_content")
public class LearningContent extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 内容编码 LC+yyyyMMdd+seq */
    private String contentCode;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 分类 1=渠道课程 2=外部课程 3=雁鸣中国 */
    private Integer category;

    /** 讲师/作者/来源 */
    private String author;

    /** 时长（视频 28:30 / 图文 约15分钟） */
    private String duration;

    /** 正文（详情页用） */
    private String body;

    /** 阅读/播放量 */
    private Integer viewCount;

    /** 角标（热/新/要闻/人物/动态/洞察） */
    private String badge;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 排序权重（越大越靠前） */
    private Integer sortOrder;

    /** 1=上架 0=下架 */
    private Integer status;
}
