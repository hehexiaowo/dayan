package com.dayan.common.sms;

import com.dayan.common.sms.dto.SmsResult;
import java.util.Map;

/**
 * 短信发送服务接口。
 *
 * <p>当前仅提供 {@link MockSmsServiceImpl}（开发态 Mock）：
 * 不真正发短信，日志打印验证码 + 将码填入 {@link SmsResult#getDevCode()} 返回，
 * 便于开发/测试阶段在 API 响应中直接看到验证码。
 *
 * <p>生产环境接入真实短信商（阿里云/腾讯云）时，新增一个实现类（如
 * {@code AliyunSmsServiceImpl}）并通过 {@code @Primary} 或 {@link org.springframework.context.annotation.Primary}
 * 覆盖 Mock 即可，调用方代码无需改动。
 */
public interface SmsService {

    /**
     * 发送短信。
     *
     * @param mobile       手机号
     * @param templateCode 模板编号（由各实现自行映射到短信商模板 ID）
     * @param params       模板参数（如验证码 {@code {"code":"123456"}}）
     * @return 发送结果
     */
    SmsResult send(String mobile, String templateCode, Map<String, String> params);
}
