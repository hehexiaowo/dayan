package com.dayan.service.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.service.dto.AssignButlerDTO;
import com.dayan.service.dto.CancelSessionDTO;
import com.dayan.service.dto.ServiceSessionCreateDTO;
import com.dayan.service.dto.ServiceSessionQueryDTO;
import com.dayan.service.dto.ServiceSessionUpdateDTO;
import com.dayan.service.dto.SubStatusUpdateDTO;
import com.dayan.service.dto.TransitionDTO;
import com.dayan.service.entity.ServiceSession;
import com.dayan.service.vo.ServiceSessionVO;

import java.util.List;

/**
 * 服务会话（service_session）服务。
 *
 * <p>核心：7 态状态机（SERVICE_SESSION_SM）+ 子状态管理 + 四环节联动动作。
 * 状态变更均经 {@link com.dayan.common.core.statemachine.StateMachineEngine}，子状态由应用层直接 UPDATE。
 */
public interface ServiceSessionService {

    // ====== 查询 ======
    PageResult<ServiceSessionVO> page(ServiceSessionQueryDTO query);

    List<ServiceSessionVO> list(ServiceSessionQueryDTO query);

    ServiceSessionVO getDetail(String sessionCode);

    /** 按编码加载会话实体，不存在抛业务异常。 */
    ServiceSession requireSession(String sessionCode);

    // ====== 创建 / 更新 / 删除 ======
    String create(ServiceSessionCreateDTO dto);

    void update(String sessionCode, ServiceSessionUpdateDTO dto);

    void delete(String sessionCode);

    // ====== 状态机业务动作（封装事件） ======

    /** 分配管家：assign_butler(1→2)，写 butlerCode/butlerFullName/acceptTime。 */
    void assignButler(AssignButlerDTO dto);

    /** 提交需求：submit_demand(2→3)。 */
    void submitDemand(String sessionCode);

    /** 确认方案：confirm_solution(3→4)，校验存在 isAccepted=1 的方案。 */
    void confirmSolution(String sessionCode);

    /** 驳回方案：reject_solution(3→2)，方案 adjustCount+1。 */
    void rejectSolution(String sessionCode);

    /** 开始服务：start_service(4→5)，校验存在 isConfirmed=1 的安排。 */
    void startService(String sessionCode);

    /** 完成服务：finish(5→6)，写 completeTime=now。 */
    void finish(String sessionCode);

    /** 取消：cancel(1/2/5→7)，写 closeReason/closeTime。 */
    void cancel(CancelSessionDTO dto);

    /** 通用状态机流转（传 sessionCode + event）。 */
    Integer transition(TransitionDTO dto);

    // ====== 子状态管理（独立于状态机） ======

    /** 更新子状态，终态校验：完成/取消且 refund_done 时拒绝再转。 */
    void updateSubStatus(SubStatusUpdateDTO dto);

    // ====== 配额聚合 ======

    /**
     * 检查权益下某服务项目的配额剩余可用次数。
     *
     * <p>配额按 equity+item 聚合统计：countConsumed(已完成) 对比 rel.quantity。
     * 年度配额（quotaType=2）只统计当年消费；终身配额（quotaType=1）统计全部消费。
     *
     * @param equityCode 权益编码
     * @param itemCode   服务项目编码
     * @param quotaType  配额周期（1=终身, 2=年度）
     * @param maxQuota   配额上限（从 rel.quantity 快照）
     * @return 剩余可用次数（>0 可用，<=0 已用尽）
     */
    int getRemainingQuota(String equityCode, String itemCode, int quotaType, int maxQuota);

    /**
     * 检查权益下某服务项目的配额剩余（完整版：激活周年锚点 + 按人配额）。
     *
     * <p>年度配额（quotaType=2）按「激活周年」统计：quota_reset_year 记录的是
     * 消费发生在激活后第几个权益年（1起），跨周年天然重置，修复一年期卡跨自然年
     * 配额翻倍的问题。anchor 为空时退化为自然年（兼容无激活时间的旧数据）。
     *
     * <p>usePersonId 非空时按权益人独立统计（goods_equity.share_mode=0 按人配额）；
     * 为空时统计全权益共享池。
     *
     * @param equityCode  权益编码
     * @param itemCode    服务项目编码
     * @param quotaType   配额周期（1=终身, 2=年度）
     * @param maxQuota    配额上限（从 rel.quantity 快照）
     * @param anchorDate  激活日期（周年锚点，null=退化自然年）
     * @param usePersonId 权益人ID（null=共享池口径）
     * @return 剩余可用次数（>0 可用，<=0 已用尽）
     */
    int getRemainingQuota(String equityCode, String itemCode, int quotaType, int maxQuota,
                          java.time.LocalDate anchorDate, Long usePersonId);
}
