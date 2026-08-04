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
    /** 渠道名称（client_account 仅存 channel_code，渠道名称由前端按需补全或留空） */
    private String channelName;
}
