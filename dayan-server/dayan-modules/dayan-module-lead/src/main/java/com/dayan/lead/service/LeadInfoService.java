package com.dayan.lead.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.lead.entity.LeadInfo;
import com.dayan.lead.vo.LeadInfoVO;

/**
 * 访客线索查询服务（读取侧）。
 */
public interface LeadInfoService {

    /**
     * 分页查询线索（可选按渠道/关键词/是否有手机号过滤）。
     *
     * @param channelCode 渠道编码（可空=全部，租户拦截器对登录端自动追加渠道条件）
     * @param keyword     姓名/手机号模糊
     * @param onlyWithPhone true 时仅看已留资
     * @param excludeClaimedByAgent 非空时排除已被认领进 agent_lead 的线索（线索池场景）
     */
    PageResult<LeadInfoVO> page(String channelCode, String keyword, Boolean onlyWithPhone,
                                boolean excludeClaimedByAgent, long current, long size);

    /**
     * 按线索编码查详情。
     */
    LeadInfoVO getByLeadCode(String leadCode);

    /**
     * 按访客令牌查实体（内部用）。
     */
    LeadInfo getByVisitorToken(String visitorToken);
}
