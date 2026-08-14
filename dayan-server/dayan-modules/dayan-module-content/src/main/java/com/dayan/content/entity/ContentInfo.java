package com.dayan.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 content_info 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_info")
public class ContentInfo extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 内容编码 */
    private String contentCode;

    /** 标题 */
    private String title;

    /** 副标题 */
    private String subtitle;

    /** 内容类型 */
    private Integer contentType;

    /** 分类编码 */
    private String categoryCode;

    /** 作者姓名 */
    private String authorName;

    /** 作者头像 */
    private String authorAvatar;

    /** 封面图URL */
    private String coverImage;

    /** 摘要 */
    private String summary;

    /** 正文内容 */
    private String contentBody;

    /** 来源类型 */
    private Integer sourceType;

    /** 来源链接 */
    private String sourceUrl;

    /** 标签 */
    private String tags;

    /** 适用业态（逗号分隔 vital/care/sojourn），空=全部 */
    private String networkTags;

    /** 是否置顶 */
    private Integer isTop;

    /** 是否推荐 */
    private Integer isRecommend;

    /** 是否允许评论 */
    private Integer isComment;

    /** 浏览次数 */
    private Integer viewCount;

    /** 点赞次数 */
    private Integer likeCount;

    /** 评论次数 */
    private Integer commentCount;

    /** 分享次数 */
    private Integer shareCount;

    /** 收藏次数 */
    private Integer collectCount;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer contentStatus;

    /** 审核状态 */
    private Integer auditStatus;

    /** 备注 */
    private String remark;
}
