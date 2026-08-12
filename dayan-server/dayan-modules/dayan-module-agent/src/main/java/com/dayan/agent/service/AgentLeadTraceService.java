package com.dayan.agent.service;

import com.dayan.agent.vo.AgentLeadTraceVO;

import java.util.List;

/**
 * 线索互动追踪服务。
 *
 * <p>处理公开分享链接打开时的访客追踪：
 * <ul>
 *   <li>按 visitor_token + agentCode 查找或自动创建线索</li>
 *   <li>记录互动明细（浏览内容/使用工具/查看海报）</li>
 *   <li>更新线索的互动统计</li>
 * </ul>
 */
public interface AgentLeadTraceService {

    /**
     * 追踪一次分享链接打开（公开，无需登录）。
     *
     * @param agentCode    代理人编码（来自 URL ?agent= 参数）
     * @param shareType    分享类型（1=内容, 2=工具, 3=海报）
     * @param bizCode      业务编码
     * @param bizTitle     展示标题
     * @param visitorToken 访客令牌（空则自动生成）
     * @param visitorSource 访客来源（wechat/browser/unknown）
     * @return 访客令牌（前端存 localStorage）
     */
    String trackVisit(String agentCode, int shareType, String bizCode,
                      String bizTitle, String visitorToken, String visitorSource);

    /**
     * 客户留资——更新匿名访客的手机号/姓名（公开，无需登录）。
     *
     * @param visitorToken 访客令牌
     * @param phone        手机号
     * @param name         姓名（可选）
     */
    void leaveContact(String visitorToken, String phone, String name);

    /**
     * 查询线索的互动时间线（代理人端，需归属校验）。
     *
     * @param leadId    线索 ID
     * @param agentCode 当前代理人编码（归属校验）
     * @return 互动记录列表（trace_time DESC，最多 50 条）
     */
    List<AgentLeadTraceVO> listByLeadId(Long leadId, String agentCode);
}
