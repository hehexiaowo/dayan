package com.dayan.client.service;

/**
 * 微信登录服务。
 *
 * <p>将前端提交的微信 code 兑换为 openId。
 *
 * <p>当前为骨架实现，未接入微信 SDK。接入时：
 * <ol>
 *   <li>pom.xml 加 {@code weixin-java-miniapp}（小程序）或 {@code weixin-java-mp}（公众号 H5）依赖</li>
 *   <li>在 application.yml 配置 appId / secret（或从 channel_open_platform 表读取）</li>
 *   <li>在此实现中调用 {@code WxMaServiceImpl.jscode2session(code)} 获取 openId</li>
 * </ol>
 */
public interface ClientWeChatLoginService {

    /**
     * 用微信登录 code 兑换 openId。
     *
     * @param code        微信登录凭证（uni.login / wx.login 返回）
     * @param channelCode 渠道编码（用于查找该渠道的微信 appId/secret 配置）
     * @return 微信 openId
     * @throws com.dayan.common.core.exception.BusinessException 未配置或兑换失败
     */
    String code2Session(String code, String channelCode);
}
