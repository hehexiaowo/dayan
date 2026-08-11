package com.dayan.common.sms.impl;

import com.dayan.common.sms.SmsService;
import com.dayan.common.sms.dto.SmsResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 开发态 Mock 短信实现。
 *
 * <p>不真正发送短信。日志打印验证码，并将验证码填入 {@link SmsResult#getDevCode()}
 * 返回给调用方，使 API 响应中携带验证码（开发/测试便利）。
 *
 * <p>接入真实短信商（阿里云/腾讯云）时，新增实现类并标注 {@code @Primary} 即可覆盖此 Mock，
 * 无需删除或修改本类。
 */
@Slf4j
@Service
public class MockSmsServiceImpl implements SmsService {

    @Override
    public SmsResult send(String mobile, String templateCode, Map<String, String> params) {
        String code = params != null ? params.get("code") : null;
        log.info("[MockSMS] 手机号={} 模板={} 验证码={}", mobile, templateCode, code);
        return SmsResult.builder()
                .success(true)
                .devCode(code)
                .build();
    }
}
