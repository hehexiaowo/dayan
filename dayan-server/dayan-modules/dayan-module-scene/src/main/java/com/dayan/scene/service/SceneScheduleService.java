package com.dayan.scene.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.scene.dto.SceneScheduleCreateDTO;
import com.dayan.scene.dto.SceneScheduleQueryDTO;
import com.dayan.scene.dto.SceneScheduleUpdateDTO;
import com.dayan.scene.vo.SceneScheduleVO;

import java.util.List;

/**
 * 场景日程服务。
 *
 * <p>按 {@code sceneCode} 维度管理日程，{@code currentPerson} 必须 ≤ {@code maxPerson}
 * （create/update 时校验）。status：1开放 / 2已约满 / 3关闭。
 */
public interface SceneScheduleService {

    PageResult<SceneScheduleVO> page(SceneScheduleQueryDTO query);

    List<SceneScheduleVO> list(SceneScheduleQueryDTO query);

    SceneScheduleVO getDetail(Long id);

    Long create(SceneScheduleCreateDTO dto);

    void update(Long id, SceneScheduleUpdateDTO dto);

    void delete(Long id);

    /**
     * 扣减排期容量（下单时调用）。
     *
     * <p>原子操作：{@code currentPerson += count}，并校验不超过 {@code maxPerson}。
     * 若排期不存在或已关闭，抛 BusinessException。
     *
     * @param scheduleId 排期 ID（对应 order_scene.schedule_code 的字符串形式）
     * @param count      参与人数
     */
    void deductCapacity(String scheduleId, int count);

    /**
     * 回补排期容量（取消订单时调用）。
     *
     * <p>原子操作：{@code currentPerson -= count}，不低于 0。
     *
     * @param scheduleId 排期 ID
     * @param count      参与人数
     */
    void restoreCapacity(String scheduleId, int count);
}
