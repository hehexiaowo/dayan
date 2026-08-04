package com.dayan.scene.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.scene.dto.SceneInfoCreateDTO;
import com.dayan.scene.dto.SceneInfoQueryDTO;
import com.dayan.scene.dto.SceneInfoUpdateDTO;
import com.dayan.scene.vo.SceneInfoVO;

import java.util.List;

/**
 * 场景信息服务。
 *
 * <p>{@code scene_info} 平台共享表，{@code sceneCode} 由后端生成（"SC" + 5 位序列），
 * {@code sceneName} 全表唯一。
 */
public interface SceneInfoService {

    PageResult<SceneInfoVO> page(SceneInfoQueryDTO query);

    List<SceneInfoVO> list(SceneInfoQueryDTO query);

    SceneInfoVO getDetail(String sceneCode);

    String create(SceneInfoCreateDTO dto);

    void update(String sceneCode, SceneInfoUpdateDTO dto);

    void delete(String sceneCode);
}
