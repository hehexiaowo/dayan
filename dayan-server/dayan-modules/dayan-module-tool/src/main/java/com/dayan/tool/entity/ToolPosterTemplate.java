package com.dayan.tool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 营销海报模板（工具域，平台共享，代理人浏览+分享）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_poster_template")
public class ToolPosterTemplate extends BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 模板编码 PT+yyyyMMdd+seq */
    private String templateCode;
    /** 标题 */
    private String title;
    /** 副标题 */
    private String subtitle;
    /** 营销正文 */
    private String bodyText;
    /** 封面/背景图 */
    private String coverImage;
    /** 分类编码 */
    private String categoryCode;
    /** 分类名称 */
    private String categoryName;
    /** 排序 */
    private Integer sortOrder;
    /** 1启用 0禁用 */
    private Integer status;
}
