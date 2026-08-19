package com.dayan.client.service;

import com.dayan.client.vo.SmsSendVO;

/**
 * Client 客户端短信验证码服务。
 *
 * <p>负责验证码的生成、Redis 存储、发送、校验与消费。
 * 与 Agent 端验证码通过 Redis scene 隔离（scene="client"）。
 */
public interface ClientSmsCodeService {

    /**
     * 发送验证码。
     *
     * <p>校验 mobile + channelCode 对应的 client_account 存在后，生成 6 位验证码，
     * 存入 Redis（5min TTL），设置 60s 重发冷却，再调 {@link com.dayan.common.sms.SmsService} 发送。
     *
     * @param mobile      手机号
     * @param channelCode 渠道编码
     * @return 发送结果（dev 环境携带 devCode）
     */
    SmsSendVO sendCode(String mobile, String channelCode);

    /**
     * 校验并消费验证码。
     *
     * <p>比对通过后立即从 Redis 删除（一次性使用）。
     *
     * @param mobile 手机号
     * @param code   用户输入的验证码
     * @return true=校验通过
     */
    boolean verifyAndConsume(String mobile, String code);
}
