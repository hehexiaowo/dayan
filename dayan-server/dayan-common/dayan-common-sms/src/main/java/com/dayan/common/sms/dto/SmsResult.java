package com.dayan.common.sms.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 短信发送结果。
 */
@Data
@Builder
public class SmsResult {

    /** 是否发送成功 */
    private boolean success;

    /**
     * 开发态验证码（仅 Mock 实现填充）。
     *
     * <p>生产实现永远返回 null。前端仅在 dev 环境读取此字段用于展示验证码，
     * 便于测试；正式环境该字段不存在。
     */
    private String devCode;

    /** 失败原因（success=false 时填充） */
    private String error;

    public static SmsResult ok() {
        return SmsResult.builder().success(true).build();
    }

    public static SmsResult fail(String error) {
        return SmsResult.builder().success(false).error(error).build();
    }
}
