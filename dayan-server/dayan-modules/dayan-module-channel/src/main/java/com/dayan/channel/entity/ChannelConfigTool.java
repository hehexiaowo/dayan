package com.dayan.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 channel_config_tool 对应实体（渠道工具配置）。
 *
 * <p>统一渠道工具配置表，对齐 channel_config_content/scene/goods 模式。
 * 所有 tool_info 的渠道级配置统一存此表，不只 aichat 知识库补充。</p>
 *
 * <p>config_type=1（问答人物知识库补充）时 config_json 格式：{@code {"repoIds": [1, 2, 3]}}</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("channel_config_tool")
public class ChannelConfigTool extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 渠道编码 */
    private String channelCode;

    /** 工具编码（tool_info.tool_code，TL 前缀） */
    private String toolCode;

    /** 配置类型（1=问答人物知识库补充） */
    private Integer configType;

    /** 配置内容 JSON（格式随 config_type 不同） */
    private String configJson;

    /** 状态（0=禁用 1=启用） */
    private Integer status;
}
