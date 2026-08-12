package com.dayan.client.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 渠道可选项（选渠道列表的列表项）。
 *
 * <p>登录前用户根据手机号/OpenID 检索关联的渠道，返回此列表供前端展示选择。
 */
@Data
@Builder
public class ChannelOptionVO {

    /** 渠道编码 */
    private String channelCode;
    /** 渠道简称（前端展示「简称（编码）」，对齐 agent 端） */
    private String shortName;
    /** 渠道全称 */
    private String fullName;
}
