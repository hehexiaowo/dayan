package com.dayan.equity.service;

import com.dayan.equity.dto.EquityBatchCreateDTO;
import com.dayan.equity.dto.EquityBatchQueryDTO;
import com.dayan.equity.dto.EquityBatchUpdateDTO;
import com.dayan.equity.entity.EquityBatch;
import com.dayan.equity.vo.EquityBatchVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 权益批次服务。
 *
 * <p>{@code batch_code}(BC+8) 唯一；统计字段由 depot 链路联动维护（{@link #incrementStat}）。
 */
public interface EquityBatchService {

    PageResult<EquityBatchVO> page(EquityBatchQueryDTO query);

    List<EquityBatchVO> list(EquityBatchQueryDTO query);

    EquityBatchVO getDetail(String batchCode);

    /** 查询实体（不存在抛业务异常），供 depot 链路使用 */
    EquityBatch requireBatch(String batchCode);

    String create(EquityBatchCreateDTO dto);

    void update(String batchCode, EquityBatchUpdateDTO dto);

    void delete(String batchCode);

    // ====== 统计字段联动（depot 核心链路调用） ======

    /**
     * 增量更新批次统计字段，SQL 形如 {@code UPDATE equity_batch SET xxx_count = xxx_count + N WHERE batch_code = ?}，
     * 避免读-改-写竞态。
     *
     * @param batchCode 批次编码
     * @param column    实体属性对应的数据库列名（如 produced_count / outbound_count / activated_count 等）
     * @param delta     增量（正负均可，出库时 remain_count -= N）
     */
    void incrementStat(String batchCode, String column, int delta);

    /**
     * 批次状态推进（直接 update batch_status）。
     */
    void updateBatchStatus(String batchCode, int newStatus);
}
