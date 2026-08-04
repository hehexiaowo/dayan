package com.dayan.channel.service;

import com.dayan.channel.dto.ChannelOpenPlatformCreateDTO;
import com.dayan.channel.dto.ChannelOpenPlatformQueryDTO;
import com.dayan.channel.dto.ChannelOpenPlatformUpdateDTO;
import com.dayan.channel.vo.ChannelOpenPlatformVO;
import com.dayan.common.core.resp.PageResult;

/**
 * 渠道开放平台配置服务。
 *
 * <p>{@code app_secret} 使用 AES-256-GCM 加密存储，密钥来源配置 {@code dayan.aes.key}，
 * 未配置时回退到 {@code AesGcmUtil.deriveKey("dayan-default-key")}。查询出参脱敏为 {@code ***}。
 */
public interface ChannelOpenPlatformService {

    PageResult<ChannelOpenPlatformVO> page(ChannelOpenPlatformQueryDTO query);

    ChannelOpenPlatformVO getDetail(Long id);

    Long create(ChannelOpenPlatformCreateDTO dto);

    void update(Long id, ChannelOpenPlatformUpdateDTO dto);

    void delete(Long id);
}
