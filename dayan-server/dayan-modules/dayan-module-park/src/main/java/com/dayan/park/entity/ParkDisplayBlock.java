package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 park_display_block 对应实体：机构展示内容板块。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_display_block")
public class ParkDisplayBlock extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 机构编码 */
    private String parkCode;

    /** 板块类型 */
    private String blockType;

    /** 板块标题 */
    private String blockTitle;

    /** 富文本内容（HTML） */
    private String content;

    /** 图片key列表（JSON数组字符串） */
    private String images;

    /** 图片描述列表（JSON数组字符串） */
    private String imageDescriptions;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态（0=隐藏, 1=显示） */
    private Integer status;

    /** 适用业态（逗号分隔 vital/care/sojourn），空=全部 */
    private String networkTags;
}
