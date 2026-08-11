package com.dayan.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 营销海报模板（后台定义，代理人浏览+分享）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("poster_template")
public class PosterTemplate extends BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String templateCode;   // 模板编码 PT+yyyyMMdd+seq
    private String title;          // 标题
    private String subtitle;       // 副标题
    private String bodyText;       // 营销正文
    private String coverImage;     // 封面/背景图
    private String categoryCode;   // 分类编码
    private String categoryName;   // 分类名称
    private Integer sortOrder;     // 排序
    private Integer status;        // 1启用 0禁用
}
