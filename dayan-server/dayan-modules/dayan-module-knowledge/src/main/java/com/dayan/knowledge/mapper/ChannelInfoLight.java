package com.dayan.knowledge.mapper;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 渠道信息轻量行（知识仓库树形继承解析用）。
 *
 * <p>直读 channel_info 表（平台共享表，租户忽略清单内）避免 knowledge → channel 模块依赖；
 * 仅取拼树与展示所需字段。
 */
@Data
@TableName("channel_info")
public class ChannelInfoLight {

    /** 渠道编码 */
    private String channelCode;

    /** 渠道全称 */
    private String fullName;

    /** 渠道简称 */
    private String shortName;

    /** 上级渠道编码（顶级为空） */
    private String parentCode;

    /** 祖级链（逗号分隔，如 "CH00001,CH00002"；不含自身） */
    private String ancestors;

    /** 层级（1=一级, 2=二级, 3=三级） */
    private Integer level;

    /** 排序号 */
    @TableField("sort_order")
    private Integer sortOrder;
}
