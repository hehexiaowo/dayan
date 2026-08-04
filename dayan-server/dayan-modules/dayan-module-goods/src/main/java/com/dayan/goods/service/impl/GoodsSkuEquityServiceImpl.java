package com.dayan.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.GoodsSkuEquityCreateDTO;
import com.dayan.goods.dto.GoodsSkuEquityQueryDTO;
import com.dayan.goods.dto.GoodsSkuEquityUpdateDTO;
import com.dayan.goods.entity.GoodsSkuEquity;
import com.dayan.goods.mapper.GoodsSkuEquityMapper;
import com.dayan.goods.service.GoodsSkuEquityService;
import com.dayan.goods.vo.GoodsSkuEquityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权益 SKU（goods_sku_equity）服务实现。
 *
 * <p>主键 id（AUTO_INCREMENT），业务键 skuCode（GE + 5 位序列，全局唯一）。
 * 关联 {@code templateCode}（权益模板）采用弱校验，不跨模块查存在性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsSkuEquityServiceImpl implements GoodsSkuEquityService {

    /** SKU 编码前缀（GE = Goods Equity） */
    private static final String SKU_PREFIX = "GE";
    private static final String SEQ_KEY = "code:seq:" + SKU_PREFIX + ":0";
    private static final int DEFAULT_STATUS = 1;

    private final GoodsSkuEquityMapper skuEquityMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<GoodsSkuEquityVO> page(GoodsSkuEquityQueryDTO query) {
        LambdaQueryWrapper<GoodsSkuEquity> wrapper = buildWrapper(query);
        Page<GoodsSkuEquity> page = skuEquityMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<GoodsSkuEquityVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<GoodsSkuEquityVO> listByGoods(String goodsCode) {
        return skuEquityMapper.selectList(new LambdaQueryWrapper<GoodsSkuEquity>()
                .eq(GoodsSkuEquity::getGoodsCode, goodsCode)
                .orderByAsc(GoodsSkuEquity::getSortOrder)
                .orderByAsc(GoodsSkuEquity::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public GoodsSkuEquityVO getDetail(Long id) {
        return toVO(requireSku(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(GoodsSkuEquityCreateDTO dto) {
        GoodsSkuEquity entity = new GoodsSkuEquity();
        entity.setGoodsCode(dto.getGoodsCode());
        entity.setSkuCode(nextSkuCode());
        entity.setSkuName(dto.getSkuName());
        entity.setTemplateCode(dto.getTemplateCode());
        entity.setEquityType(dto.getEquityType());
        entity.setEquityValue(dto.getEquityValue());
        entity.setSkuPrice(dto.getSkuPrice());
        entity.setStock(dto.getStock());
        entity.setSalesCount(0);
        entity.setSpecDescription(dto.getSpecDescription());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());

        skuEquityMapper.insert(entity);
        log.info("创建权益 SKU 成功: goodsCode={}, skuCode={}, id={}",
                dto.getGoodsCode(), entity.getSkuCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, GoodsSkuEquityUpdateDTO dto) {
        GoodsSkuEquity existing = requireSku(id);
        GoodsSkuEquity update = new GoodsSkuEquity();
        update.setId(existing.getId());

        if (dto.getSkuName() != null) update.setSkuName(dto.getSkuName());
        if (dto.getTemplateCode() != null) update.setTemplateCode(dto.getTemplateCode());
        if (dto.getEquityType() != null) update.setEquityType(dto.getEquityType());
        if (dto.getEquityValue() != null) update.setEquityValue(dto.getEquityValue());
        if (dto.getSkuPrice() != null) update.setSkuPrice(dto.getSkuPrice());
        if (dto.getStock() != null) update.setStock(dto.getStock());
        if (dto.getSpecDescription() != null) update.setSpecDescription(dto.getSpecDescription());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());

        skuEquityMapper.updateById(update);
        log.info("更新权益 SKU 成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        GoodsSkuEquity existing = requireSku(id);
        skuEquityMapper.deleteById(existing.getId());
        log.info("删除权益 SKU 成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<GoodsSkuEquity> buildWrapper(GoodsSkuEquityQueryDTO query) {
        LambdaQueryWrapper<GoodsSkuEquity> wrapper = new LambdaQueryWrapper<GoodsSkuEquity>()
                .orderByAsc(GoodsSkuEquity::getSortOrder)
                .orderByAsc(GoodsSkuEquity::getId);
        if (query.getGoodsCode() != null && !query.getGoodsCode().isEmpty()) {
            wrapper.eq(GoodsSkuEquity::getGoodsCode, query.getGoodsCode());
        }
        if (query.getSkuCode() != null && !query.getSkuCode().isEmpty()) {
            wrapper.eq(GoodsSkuEquity::getSkuCode, query.getSkuCode());
        }
        if (query.getSkuName() != null && !query.getSkuName().isEmpty()) {
            wrapper.like(GoodsSkuEquity::getSkuName, query.getSkuName());
        }
        if (query.getTemplateCode() != null && !query.getTemplateCode().isEmpty()) {
            wrapper.eq(GoodsSkuEquity::getTemplateCode, query.getTemplateCode());
        }
        if (query.getEquityType() != null) {
            wrapper.eq(GoodsSkuEquity::getEquityType, query.getEquityType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(GoodsSkuEquity::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private GoodsSkuEquity requireSku(Long id) {
        GoodsSkuEquity entity = skuEquityMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权益 SKU 不存在: id=" + id);
        }
        return entity;
    }

    private String nextSkuCode() {
        return SKU_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY));
    }

    private GoodsSkuEquityVO toVO(GoodsSkuEquity entity) {
        GoodsSkuEquityVO vo = new GoodsSkuEquityVO();
        vo.setId(entity.getId());
        vo.setGoodsCode(entity.getGoodsCode());
        vo.setSkuCode(entity.getSkuCode());
        vo.setSkuName(entity.getSkuName());
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setEquityType(entity.getEquityType());
        vo.setEquityValue(entity.getEquityValue());
        vo.setSkuPrice(entity.getSkuPrice());
        vo.setStock(entity.getStock());
        vo.setSalesCount(entity.getSalesCount());
        vo.setSpecDescription(entity.getSpecDescription());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
