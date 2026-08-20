package com.dayan.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 channel_config_course 对应实体（渠道课程配置）。
 *
 * <p>渠道课程可见性配置表，对齐 channel_config_tool/content/scene/goods 模式。
 * config_type=0（基础可见性）时 config_json 默认为 '{}'，预留扩展。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("channel_config_course")
public class ChannelConfigCourse extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 渠道编码 */
    private String channelCode;

    /** 课程编码（course_info.course_code） */
    private String courseCode;

    /** 配置类型（0=基础可见性，预留扩展） */
    private Integer configType;

    /** 配置内容 JSON（格式随 config_type 不同） */
    private String configJson;

    /** 状态（0=禁用 1=启用） */
    private Integer status;
}
