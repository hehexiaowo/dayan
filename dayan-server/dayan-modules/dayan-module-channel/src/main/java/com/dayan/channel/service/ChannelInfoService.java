package com.dayan.channel.service;

import com.dayan.channel.dto.ChannelInfoCreateDTO;
import com.dayan.channel.dto.ChannelInfoQueryDTO;
import com.dayan.channel.dto.ChannelInfoUpdateDTO;
import com.dayan.channel.vo.ChannelInfoVO;

import java.util.List;

/**
 * 渠道信息（树形）服务。
 *
 * <p>{@code channel_info} 为平台共享表（DayanTenantHandler 忽略），不参与渠道字段隔离。
 * 树形结构通过 {@code parent_code} + {@code ancestors} 维护。
 */
public interface ChannelInfoService {

    /**
     * 按父渠道查询子渠道列表（平铺）。
     *
     * @param query 查询条件；{@code parentCode} 为空返回全部
     */
    List<ChannelInfoVO> listByParent(ChannelInfoQueryDTO query);

    /**
     * 查询全部渠道并构建树（以 {@code parentCode} 为空/不存在的节点为根）。
     */
    List<ChannelInfoVO> tree();

    /**
     * 渠道详情。
     */
    ChannelInfoVO getDetail(String channelCode);

    /**
     * 新增渠道，返回生成的 channelCode。
     */
    String create(ChannelInfoCreateDTO dto);

    /**
     * 修改渠道（含移动层级：变更 parentCode 时同步重算 ancestors/level）。
     */
    void update(String channelCode, ChannelInfoUpdateDTO dto);

    /**
     * 删除渠道（校验子渠道存在性）。
     */
    void delete(String channelCode);

    /** 校验当前渠道有管理能力，否则抛 BusinessException */
    void requireManageCapability();

    /** 校验 targetChannelCode 是当前渠道的后代（ancestors 含当前渠道），否则抛越权异常 */
    void requireDescendant(String targetChannelCode);
}
