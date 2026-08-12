package com.dayan.client.vo;

import lombok.Builder;
import lombok.Data;

/**
 * Client 客户端登录成功响应。
 */
@Data
@Builder
public class ClientLoginVO {

    /** Sa-Token 签发的 Token 值 */
    private String token;
    /** Token 请求头名称（Client-Token） */
    private String tokenName;
    /** 客户编码 */
    private String clientCode;
    /** 客户姓名（client_account.username，前端展示用） */
    private String clientName;
    /** 所属渠道编码 */
    private String channelCode;
}
