package com.dayan.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.goods.dto.GoodsSkuCourseCreateDTO;
import com.dayan.goods.dto.GoodsSkuCourseQueryDTO;
import com.dayan.goods.dto.GoodsSkuCourseUpdateDTO;
import com.dayan.goods.entity.GoodsSkuCourse;
import com.dayan.goods.mapper.GoodsSkuCourseMapper;
import com.dayan.goods.service.GoodsSkuCourseService;
import com.dayan.goods.vo.GoodsSkuCourseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程 SKU（goods_sku_course）服务实现。
 *
 * <p>主键 id（AUTO_INCREMENT），业务键 skuCode（GC + 5 位序列）。
 * 关联 {@code courseCode}（课程）采用弱校验。
 *
 * <p>规格约定"maxStudents 学员上限"，表结构未单独建该字段，以 {@code stock}（库存）承载学员上限语义。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsSkuCourseServiceImpl implements GoodsSkuCourseService {

    /** SKU 编码前缀（GC = Goods Course） */
    private static final String SKU_PREFIX = "GC";
    private static final String SEQ_KEY = "code:seq:" + SKU_PREFIX + ":0";
    private static final int DEFAULT_STATUS = 1;

    private final GoodsSkuCourseMapper skuCourseMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<GoodsSkuCourseVO> page(GoodsSkuCourseQueryDTO query) {
        LambdaQueryWrapper<GoodsSkuCourse> wrapper = buildWrapper(query);
        Page<GoodsSkuCourse> page = skuCourseMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<GoodsSkuCourseVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<GoodsSkuCourseVO> listByGoods(String goodsCode) {
        return skuCourseMapper.selectList(new LambdaQueryWrapper<GoodsSkuCourse>()
                .eq(GoodsSkuCourse::getGoodsCode, goodsCode)
                .orderByAsc(GoodsSkuCourse::getSortOrder)
                .orderByAsc(GoodsSkuCourse::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public GoodsSkuCourseVO getDetail(Long id) {
        return toVO(requireSku(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(GoodsSkuCourseCreateDTO dto) {
        GoodsSkuCourse entity = new GoodsSkuCourse();
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

        skuCourseMapper.insert(entity);
        log.info("创建课程 SKU 成功: goodsCode={}, skuCode={}, id={}",
                dto.getGoodsCode(), entity.getSkuCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, GoodsSkuCourseUpdateDTO dto) {
        GoodsSkuCourse existing = requireSku(id);
        GoodsSkuCourse update = new GoodsSkuCourse();
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

        skuCourseMapper.updateById(update);
        log.info("更新课程 SKU 成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        GoodsSkuCourse existing = requireSku(id);
        skuCourseMapper.deleteById(existing.getId());
        log.info("删除课程 SKU 成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<GoodsSkuCourse> buildWrapper(GoodsSkuCourseQueryDTO query) {
        LambdaQueryWrapper<GoodsSkuCourse> wrapper = new LambdaQueryWrapper<GoodsSkuCourse>()
                .orderByAsc(GoodsSkuCourse::getSortOrder)
                .orderByAsc(GoodsSkuCourse::getId);
        if (query.getGoodsCode() != null && !query.getGoodsCode().isEmpty()) {
            wrapper.eq(GoodsSkuCourse::getGoodsCode, query.getGoodsCode());
        }
        if (query.getSkuCode() != null && !query.getSkuCode().isEmpty()) {
            wrapper.eq(GoodsSkuCourse::getSkuCode, query.getSkuCode());
        }
        if (query.getSkuName() != null && !query.getSkuName().isEmpty()) {
            wrapper.like(GoodsSkuCourse::getSkuName, query.getSkuName());
        }
        if (query.getCourseCode() != null && !query.getCourseCode().isEmpty()) {
            wrapper.eq(GoodsSkuCourse::getCourseCode, query.getCourseCode());
        }
        if (query.getCourseType() != null) {
            wrapper.eq(GoodsSkuCourse::getCourseType, query.getCourseType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(GoodsSkuCourse::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private GoodsSkuCourse requireSku(Long id) {
        GoodsSkuCourse entity = skuCourseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "课程 SKU 不存在: id=" + id);
        }
        return entity;
    }

    private String nextSkuCode() {
        return SKU_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY));
    }

    private GoodsSkuCourseVO toVO(GoodsSkuCourse entity) {
        GoodsSkuCourseVO vo = new GoodsSkuCourseVO();
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
