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
}
