package com.dayan.scene.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.scene.dto.SceneItemCreateDTO;
import com.dayan.scene.dto.SceneItemQueryDTO;
import com.dayan.scene.dto.SceneItemUpdateDTO;
import com.dayan.scene.vo.SceneItemVO;

import java.util.List;

/**
 * 场景项目服务。
 *
 * <p>按 {@code sceneCode} 维度管理场景下的项目明细，{@code itemCode} 同场景内唯一。
 */
public interface SceneItemService {

    PageResult<SceneItemVO> page(SceneItemQueryDTO query);

    List<SceneItemVO> list(SceneItemQueryDTO query);

    SceneItemVO getDetail(Long id);

    Long create(SceneItemCreateDTO dto);

    void update(Long id, SceneItemUpdateDTO dto);

    void delete(Long id);
}
