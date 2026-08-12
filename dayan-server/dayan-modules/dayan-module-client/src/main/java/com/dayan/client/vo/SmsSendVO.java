package com.dayan.client.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 短信验证码发送结果。
 */
@Data
@Builder
public class SmsSendVO {

    /** 是否发送成功 */
    private boolean sent;

    /**
     * 开发态验证码（仅 Mock SMS 实现时填充）。
     *
     * <p>生产环境此字段为 null。前端可据此在 dev 环境 toast 显示验证码。
     */
    private String devCode;
}
