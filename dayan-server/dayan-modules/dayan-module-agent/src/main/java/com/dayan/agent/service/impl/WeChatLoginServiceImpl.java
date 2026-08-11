package com.dayan.agent.service.impl;

import com.dayan.agent.service.WeChatLoginService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 微信登录服务骨架实现。
 *
 * <p><b>未接入微信 SDK</b> —— 调用时直接抛「微信登录未配置」异常。
 *
 * <p>接入步骤：
 * <ol>
 *   <li>pom.xml 加 {@code com.github.binarywang:weixin-java-miniapp}（小程序）依赖</li>
 *   <li>application.yml 配置 {@code dayan.wechat.miniapp.app-id / app-secret}</li>
 *   <li>替换此实现：注入 {@code WxMaService}，调用 {@code jscode2session(code)} 获取 openId</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatLoginServiceImpl implements WeChatLoginService {

    @Override
    public String code2Session(String code, String channelCode) {
        log.warn("微信登录未配置：code={}, channelCode={}。请在 application.yml 配置 appId/secret 并接入 weixin-java-miniapp SDK",
                code, channelCode);
        throw new BusinessException(ErrorCode.BUSINESS, "微信登录暂未开通，请联系管理员");
    }
}
