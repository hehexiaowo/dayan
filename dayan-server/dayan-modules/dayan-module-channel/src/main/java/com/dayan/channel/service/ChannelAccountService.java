package com.dayan.channel.service;

import com.dayan.channel.dto.ChannelAccountCreateDTO;
import com.dayan.channel.dto.ChannelAccountQueryDTO;
import com.dayan.channel.dto.ChannelAccountUpdateDTO;
import com.dayan.channel.vo.ChannelAccountVO;
import com.dayan.common.core.resp.PageResult;

/**
 * 渠道账号管理服务。
 *
 * <p>{@code channel_account} 受渠道字段隔离（DayanTenantHandler 自动追加 channel_code 条件），
 * Admin 端查询时通过 {@link com.dayan.common.mybatis.context.ContextHolder} 写入 channelCode。
 */
public interface ChannelAccountService {

    PageResult<ChannelAccountVO> page(ChannelAccountQueryDTO query);

    ChannelAccountVO getDetail(String accountCode);

    String create(ChannelAccountCreateDTO dto);

    void update(String accountCode, ChannelAccountUpdateDTO dto);

    /** 重置密码为默认值 */
    void resetPassword(String accountCode);

    void delete(String accountCode);
}
