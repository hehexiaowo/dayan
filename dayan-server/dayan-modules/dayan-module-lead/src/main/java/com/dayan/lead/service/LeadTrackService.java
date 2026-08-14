package com.dayan.lead.service;

import com.dayan.lead.vo.LeadTraceVO;

import java.util.List;

/**
 * 访客线索追踪服务（公开触点写入侧）。
 *
 * <p>由 agent 端公开分享接口（/open/share/*）调用：分享页打开时 track，
 * 留资时 leaveContact。线索归属渠道，不绑定代理人；分享人渠道/代理人
 * 信息记录在三类互动明细表中。
 */
public interface LeadTrackService {

    /**
     * 追踪一次分享触点：按 visitorToken 查/建 lead_info，写入对应互动记录，
     * 并更新线索的互动聚合字段。
     *
     * @param channelCode   渠道编码（分享人所属渠道）
     * @param agentCode     分享人代理人编码（可空=直接访问）
     * @param interactType  互动类型（1=内容 2=工具 3=海报）
     * @param bizCode       业务编码（contentCode/toolCode/templateCode）
     * @param bizTitle      业务标题（冗余快照）
     * @param visitorToken  访客令牌（空则生成新的）
     * @param visitorSource 访客环境来源（wechat/browser）
     * @return 访客令牌（新生成或原样返回）
     */
    String track(String channelCode, String agentCode, int interactType, String bizCode,
                 String bizTitle, String visitorToken, String visitorSource);

    /**
     * 留资：回填手机号/姓名到 lead_info。
     */
    void leaveContact(String visitorToken, String phone, String name);

    /**
     * 绑定客户：留资/注册建档后回填 client_code。
     */
    void bindClient(String visitorToken, String clientCode);

    /**
     * 按访客令牌查互动时间线（三类记录合并，按时间倒序）。
     */
    List<LeadTraceVO> listTraces(String visitorToken, int limit);
}
