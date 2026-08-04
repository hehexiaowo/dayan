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
}
