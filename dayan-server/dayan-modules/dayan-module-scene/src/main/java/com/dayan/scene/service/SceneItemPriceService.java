package com.dayan.scene.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.scene.dto.SceneItemPriceCreateDTO;
import com.dayan.scene.dto.SceneItemPriceQueryDTO;
import com.dayan.scene.dto.SceneItemPriceUpdateDTO;
import com.dayan.scene.vo.SceneItemPriceVO;

import java.util.List;

/**
 * 场景项目定价服务。
 *
 * <p>按 {@code sceneCode + sceneItemCode} 维度管理定价，
 * {@code channelPrice} 提供渠道差异化定价字段。
 */
public interface SceneItemPriceService {

    PageResult<SceneItemPriceVO> page(SceneItemPriceQueryDTO query);

    List<SceneItemPriceVO> list(SceneItemPriceQueryDTO query);

    SceneItemPriceVO getDetail(Long id);

    Long create(SceneItemPriceCreateDTO dto);

    void update(Long id, SceneItemPriceUpdateDTO dto);

    void delete(Long id);
}
