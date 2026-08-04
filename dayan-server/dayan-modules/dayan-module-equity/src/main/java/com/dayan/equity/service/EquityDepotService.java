package com.dayan.equity.service;

import com.dayan.equity.dto.ActivateDTO;
import com.dayan.equity.dto.ChangeDoneDTO;
import com.dayan.equity.dto.ChangeHolderDTO;
import com.dayan.equity.dto.ChangeRollbackDTO;
import com.dayan.equity.dto.EquityDepotQueryDTO;
import com.dayan.equity.dto.OutboundDTO;
import com.dayan.equity.dto.StockInDTO;
import com.dayan.equity.dto.VoidDTO;
import com.dayan.equity.entity.EquityDepot;
import com.dayan.equity.vo.EquityDepotVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 权益卡/函（equity_depot）服务 —— 核心链路。
 *
 * <p>承载权益全生命周期：入库 → 出库 → 激活 → 使用 → 完成 / 过期 / 作废 / 更换权益人。
 * 所有 equity_status 变更必须经 {@code StateMachineEngine.transition("EQUITY_SM", from, event)}。
 */
public interface EquityDepotService {

    PageResult<EquityDepotVO> page(EquityDepotQueryDTO query);

    List<EquityDepotVO> list(EquityDepotQueryDTO query);

    EquityDepotVO getDetail(String equityCode);

    /** 查询实体（不存在抛业务异常） */
    EquityDepot requireEquity(String equityCode);

    // ====== 核心链路 ======

    /** 批量入库（stockIn）：生成 N 张权益 + 联动批次统计/状态 */
    int stockIn(StockInDTO dto);

    /** 出库（outbound）：批量 0→1 + 联动 outbound_count */
    int outbound(OutboundDTO dto);

    /** 激活（activate）：1→2 + 写 activate_time/expire_time + 插激活记录 + 联动 activated_count + 自动建默认使用人 */
    String activate(ActivateDTO dto);

    /** 作废（void）：0/1→6 + 联动 voided_count/remain_count */
    void voidEquity(VoidDTO dto);

    // ====== 更换权益人（核心链路） ======

    /** 发起更换权益人：2→7 + 插 change_holder 记录（待处理） */
    Long changeHolder(ChangeHolderDTO dto);

    /** 完成更换：7→2 + 更新 change_status=1 + 切换默认使用人 */
    void changeDone(ChangeDoneDTO dto);

    /** 回滚更换：7→2（复用 change_done 事件） + 更新 change_status=2 */
    void changeRollback(ChangeRollbackDTO dto);

    // ====== 状态机通用流转（服务会话/完成等扩展接口，供后续阶段调用） ======

    /**
     * 通用状态机流转：校验并取得目标状态后落库，不动其它字段。
     *
     * @param equityCode 权益编码
     * @param event      事件（须与 EQUITY_SM 种子一致）
     * @return 目标状态 to
     */
    Integer transition(String equityCode, String event);
}
