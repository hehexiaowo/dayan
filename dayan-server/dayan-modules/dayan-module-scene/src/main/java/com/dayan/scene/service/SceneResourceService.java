package com.dayan.scene.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.scene.dto.SceneResourceCreateDTO;
import com.dayan.scene.dto.SceneResourceQueryDTO;
import com.dayan.scene.dto.SceneResourceUpdateDTO;
import com.dayan.scene.vo.SceneResourceVO;

import java.util.List;

/**
 * 场景资源服务。
 *
 * <p>按 {@code sceneCode} 维度管理场景资源。资源冲突检测（应用层校验）：
 * 同 {@code sceneCode} 下，重复的资源标识（{@code resourceName + resourceType}）视为冲突，
 * 抛 BusinessException。
 */
public interface SceneResourceService {

    PageResult<SceneResourceVO> page(SceneResourceQueryDTO query);

    List<SceneResourceVO> list(SceneResourceQueryDTO query);

    SceneResourceVO getDetail(Long id);

    Long create(SceneResourceCreateDTO dto);

    void update(Long id, SceneResourceUpdateDTO dto);

    void delete(Long id);
}
