package com.dayan.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.GoodsSceneCreateDTO;
import com.dayan.goods.dto.GoodsSceneQueryDTO;
import com.dayan.goods.dto.GoodsSceneUpdateDTO;
import com.dayan.goods.entity.GoodsScene;
import com.dayan.goods.mapper.GoodsSceneMapper;
import com.dayan.goods.service.GoodsSceneService;
import com.dayan.goods.vo.GoodsSceneVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 场景 SKU（goods_scene）服务实现。
 *
 * <p>主键 id（AUTO_INCREMENT），业务键 skuCode（GS + 5 位序列）。
 * 关联 {@code sceneCode}（场景）/ {@code parkCode}（机构）采用弱校验。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsSceneServiceImpl implements GoodsSceneService {

    /** SKU 编码前缀（GS = Goods Scene） */
    private static final String SKU_PREFIX = "GS";
    private static final String SEQ_KEY = "code:seq:" + SKU_PREFIX + ":0";
    private static final int DEFAULT_STATUS = 1;

    private final GoodsSceneMapper sceneMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<GoodsSceneVO> page(GoodsSceneQueryDTO query) {
        LambdaQueryWrapper<GoodsScene> wrapper = buildWrapper(query);
        Page<GoodsScene> page = sceneMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<GoodsSceneVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<GoodsSceneVO> listByGoods(String goodsCode) {
        return sceneMapper.selectList(new LambdaQueryWrapper<GoodsScene>()
                .eq(GoodsScene::getGoodsCode, goodsCode)
                .orderByAsc(GoodsScene::getSortOrder)
                .orderByAsc(GoodsScene::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public GoodsSceneVO getDetail(Long id) {
        return toVO(requireSku(id));
    }

    @Override
    public GoodsSceneVO getByCode(String skuCode) {
        GoodsScene entity = sceneMapper.selectOne(new LambdaQueryWrapper<GoodsScene>()
                .eq(GoodsScene::getSkuCode, skuCode)
                .last("LIMIT 1"));
        return entity != null ? toVO(entity) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(GoodsSceneCreateDTO dto) {
        GoodsScene entity = new GoodsScene();
        entity.setGoodsCode(dto.getGoodsCode());
        entity.setSkuCode(nextSkuCode());
        entity.setSkuName(dto.getSkuName());
        entity.setSceneCode(dto.getSceneCode());
        entity.setParkCode(dto.getParkCode());
        entity.setSkuPrice(dto.getSkuPrice());
        entity.setPersonLimit(dto.getPersonLimit());
        entity.setDurationHours(dto.getDurationHours());
        entity.setScheduleDescription(dto.getScheduleDescription());
        entity.setStock(dto.getStock());
        entity.setSalesCount(0);
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());

        sceneMapper.insert(entity);
        log.info("创建场景 SKU 成功: goodsCode={}, skuCode={}, id={}",
                dto.getGoodsCode(), entity.getSkuCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, GoodsSceneUpdateDTO dto) {
        GoodsScene existing = requireSku(id);
        GoodsScene update = new GoodsScene();
        update.setId(existing.getId());

        if (dto.getSkuName() != null) update.setSkuName(dto.getSkuName());
        if (dto.getSceneCode() != null) update.setSceneCode(dto.getSceneCode());
        if (dto.getParkCode() != null) update.setParkCode(dto.getParkCode());
        if (dto.getSkuPrice() != null) update.setSkuPrice(dto.getSkuPrice());
        if (dto.getPersonLimit() != null) update.setPersonLimit(dto.getPersonLimit());
        if (dto.getDurationHours() != null) update.setDurationHours(dto.getDurationHours());
        if (dto.getScheduleDescription() != null) update.setScheduleDescription(dto.getScheduleDescription());
        if (dto.getStock() != null) update.setStock(dto.getStock());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());

        sceneMapper.updateById(update);
        log.info("更新场景 SKU 成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        GoodsScene existing = requireSku(id);
        sceneMapper.deleteById(existing.getId());
        log.info("删除场景 SKU 成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<GoodsScene> buildWrapper(GoodsSceneQueryDTO query) {
        LambdaQueryWrapper<GoodsScene> wrapper = new LambdaQueryWrapper<GoodsScene>()
                .orderByAsc(GoodsScene::getSortOrder)
                .orderByAsc(GoodsScene::getId);
        if (query.getGoodsCode() != null && !query.getGoodsCode().isEmpty()) {
            wrapper.eq(GoodsScene::getGoodsCode, query.getGoodsCode());
        }
        if (query.getSkuCode() != null && !query.getSkuCode().isEmpty()) {
            wrapper.eq(GoodsScene::getSkuCode, query.getSkuCode());
        }
        if (query.getSkuName() != null && !query.getSkuName().isEmpty()) {
            wrapper.like(GoodsScene::getSkuName, query.getSkuName());
        }
        if (query.getSceneCode() != null && !query.getSceneCode().isEmpty()) {
            wrapper.eq(GoodsScene::getSceneCode, query.getSceneCode());
        }
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(GoodsScene::getParkCode, query.getParkCode());
        }
        if (query.getStatus() != null) {
            wrapper.eq(GoodsScene::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private GoodsScene requireSku(Long id) {
        GoodsScene entity = sceneMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "场景 SKU 不存在: id=" + id);
        }
        return entity;
    }

    private String nextSkuCode() {
        return SKU_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY));
    }

    private GoodsSceneVO toVO(GoodsScene entity) {
        GoodsSceneVO vo = new GoodsSceneVO();
        vo.setId(entity.getId());
        vo.setGoodsCode(entity.getGoodsCode());
        vo.setSkuCode(entity.getSkuCode());
        vo.setSkuName(entity.getSkuName());
        vo.setSceneCode(entity.getSceneCode());
        vo.setParkCode(entity.getParkCode());
        vo.setSkuPrice(entity.getSkuPrice());
        vo.setPersonLimit(entity.getPersonLimit());
        vo.setDurationHours(entity.getDurationHours());
        vo.setScheduleDescription(entity.getScheduleDescription());
        vo.setStock(entity.getStock());
        vo.setSalesCount(entity.getSalesCount());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
