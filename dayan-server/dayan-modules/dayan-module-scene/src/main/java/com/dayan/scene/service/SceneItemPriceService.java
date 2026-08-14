package com.dayan.scene.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.scene.dto.SceneItemPriceCreateDTO;
import com.dayan.scene.dto.SceneItemPriceQueryDTO;
import com.dayan.scene.dto.SceneItemPriceUpdateDTO;
import com.dayan.scene.vo.SceneItemPriceVO;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    /**
     * 取场景在指定日期的当前有效按人价（status=1, price_type=1, 日期窗口内）。
     * channelPrice 非空优先；不存在返回 null。
     */
    BigDecimal getCurrentPersonPrice(String sceneCode, LocalDate activeOn);

    Long create(SceneItemPriceCreateDTO dto);

    void update(Long id, SceneItemPriceUpdateDTO dto);

    void delete(Long id);
}
