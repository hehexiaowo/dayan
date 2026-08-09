package com.dayan.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.GoodsCourseCreateDTO;
import com.dayan.goods.dto.GoodsCourseQueryDTO;
import com.dayan.goods.dto.GoodsCourseUpdateDTO;
import com.dayan.goods.entity.GoodsCourse;
import com.dayan.goods.mapper.GoodsCourseMapper;
import com.dayan.goods.service.GoodsCourseService;
import com.dayan.goods.vo.GoodsCourseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程 SKU（goods_course）服务实现。
 *
 * <p>主键 id（AUTO_INCREMENT），业务键 skuCode（GC + 5 位序列）。
 * 关联 {@code courseCode}（课程）采用弱校验。
 *
 * <p>规格约定"maxStudents 学员上限"，表结构未单独建该字段，以 {@code stock}（库存）承载学员上限语义。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsCourseServiceImpl implements GoodsCourseService {

    /** SKU 编码前缀（GC = Goods Course） */
    private static final String SKU_PREFIX = "GC";
    private static final String SEQ_KEY = "code:seq:" + SKU_PREFIX + ":0";
    private static final int DEFAULT_STATUS = 1;

    private final GoodsCourseMapper courseMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<GoodsCourseVO> page(GoodsCourseQueryDTO query) {
        LambdaQueryWrapper<GoodsCourse> wrapper = buildWrapper(query);
        Page<GoodsCourse> page = courseMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<GoodsCourseVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<GoodsCourseVO> listByGoods(String goodsCode) {
        return courseMapper.selectList(new LambdaQueryWrapper<GoodsCourse>()
                .eq(GoodsCourse::getGoodsCode, goodsCode)
                .orderByAsc(GoodsCourse::getSortOrder)
                .orderByAsc(GoodsCourse::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public GoodsCourseVO getDetail(Long id) {
        return toVO(requireSku(id));
    }

    @Override
    public GoodsCourseVO getByCode(String skuCode) {
        GoodsCourse entity = courseMapper.selectOne(new LambdaQueryWrapper<GoodsCourse>()
                .eq(GoodsCourse::getSkuCode, skuCode)
                .last("LIMIT 1"));
        return entity != null ? toVO(entity) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(GoodsCourseCreateDTO dto) {
        GoodsCourse entity = new GoodsCourse();
        entity.setGoodsCode(dto.getGoodsCode());
        entity.setSkuCode(nextSkuCode());
        entity.setSkuName(dto.getSkuName());
        entity.setCourseCode(dto.getCourseCode());
        entity.setCourseType(dto.getCourseType());
        entity.setSkuPrice(dto.getSkuPrice());
        entity.setClassCount(dto.getClassCount());
        entity.setValidDays(dto.getValidDays());
        entity.setStock(dto.getStock());
        entity.setSalesCount(0);
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());

        courseMapper.insert(entity);
        log.info("创建课程 SKU 成功: goodsCode={}, skuCode={}, id={}",
                dto.getGoodsCode(), entity.getSkuCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, GoodsCourseUpdateDTO dto) {
        GoodsCourse existing = requireSku(id);
        GoodsCourse update = new GoodsCourse();
        update.setId(existing.getId());

        if (dto.getSkuName() != null) update.setSkuName(dto.getSkuName());
        if (dto.getCourseCode() != null) update.setCourseCode(dto.getCourseCode());
        if (dto.getCourseType() != null) update.setCourseType(dto.getCourseType());
        if (dto.getSkuPrice() != null) update.setSkuPrice(dto.getSkuPrice());
        if (dto.getClassCount() != null) update.setClassCount(dto.getClassCount());
        if (dto.getValidDays() != null) update.setValidDays(dto.getValidDays());
        if (dto.getStock() != null) update.setStock(dto.getStock());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());

        courseMapper.updateById(update);
        log.info("更新课程 SKU 成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        GoodsCourse existing = requireSku(id);
        courseMapper.deleteById(existing.getId());
        log.info("删除课程 SKU 成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<GoodsCourse> buildWrapper(GoodsCourseQueryDTO query) {
        LambdaQueryWrapper<GoodsCourse> wrapper = new LambdaQueryWrapper<GoodsCourse>()
                .orderByAsc(GoodsCourse::getSortOrder)
                .orderByAsc(GoodsCourse::getId);
        if (query.getGoodsCode() != null && !query.getGoodsCode().isEmpty()) {
            wrapper.eq(GoodsCourse::getGoodsCode, query.getGoodsCode());
        }
        if (query.getSkuCode() != null && !query.getSkuCode().isEmpty()) {
            wrapper.eq(GoodsCourse::getSkuCode, query.getSkuCode());
        }
        if (query.getSkuName() != null && !query.getSkuName().isEmpty()) {
            wrapper.like(GoodsCourse::getSkuName, query.getSkuName());
        }
        if (query.getCourseCode() != null && !query.getCourseCode().isEmpty()) {
            wrapper.eq(GoodsCourse::getCourseCode, query.getCourseCode());
        }
        if (query.getCourseType() != null) {
            wrapper.eq(GoodsCourse::getCourseType, query.getCourseType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(GoodsCourse::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private GoodsCourse requireSku(Long id) {
        GoodsCourse entity = courseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "课程 SKU 不存在: id=" + id);
        }
        return entity;
    }

    private String nextSkuCode() {
        return SKU_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY));
    }

    private GoodsCourseVO toVO(GoodsCourse entity) {
        GoodsCourseVO vo = new GoodsCourseVO();
        vo.setId(entity.getId());
        vo.setGoodsCode(entity.getGoodsCode());
        vo.setSkuCode(entity.getSkuCode());
        vo.setSkuName(entity.getSkuName());
        vo.setCourseCode(entity.getCourseCode());
        vo.setCourseType(entity.getCourseType());
        vo.setSkuPrice(entity.getSkuPrice());
        vo.setClassCount(entity.getClassCount());
        vo.setValidDays(entity.getValidDays());
        vo.setStock(entity.getStock());
        vo.setSalesCount(entity.getSalesCount());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
