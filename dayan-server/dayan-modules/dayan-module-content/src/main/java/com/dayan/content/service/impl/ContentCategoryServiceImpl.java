package com.dayan.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.content.dto.ContentCategoryCreateDTO;
import com.dayan.content.dto.ContentCategoryQueryDTO;
import com.dayan.content.dto.ContentCategoryUpdateDTO;
import com.dayan.content.entity.ContentCategory;
import com.dayan.content.mapper.ContentCategoryMapper;
import com.dayan.content.service.ContentCategoryService;
import com.dayan.content.vo.ContentCategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 内容分类服务实现。
 *
 * <p>平铺维护为主：按 {@code categoryName} 排序，{@code categoryCode} 可选（入参未提供时留空）。
 * {@code content_category} 平台共享表，不受渠道字段隔离约束。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentCategoryServiceImpl implements ContentCategoryService {

    private final ContentCategoryMapper contentCategoryMapper;

    @Override
    public PageResult<ContentCategoryVO> page(ContentCategoryQueryDTO query) {
        LambdaQueryWrapper<ContentCategory> wrapper = buildWrapper(query);
        Page<ContentCategory> page = contentCategoryMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ContentCategoryVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ContentCategoryVO> list(ContentCategoryQueryDTO query) {
        return contentCategoryMapper.selectList(buildWrapper(query)).stream()
                .map(this::toVO).toList();
    }

    @Override
    public ContentCategoryVO getDetail(Long id) {
        return toVO(requireById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ContentCategoryCreateDTO dto) {
        ContentCategory entity = new ContentCategory();
        entity.setCategoryCode(dto.getCategoryCode());
        entity.setCategoryName(dto.getCategoryName());
        entity.setParentCode(dto.getParentCode());
        entity.setCategoryType(dto.getCategoryType());
        entity.setIcon(dto.getIcon());
        entity.setCoverImage(dto.getCoverImage());
        entity.setDescription(dto.getDescription());
        entity.setContentCount(0);
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setIsVisible(dto.getIsVisible() == null ? 1 : dto.getIsVisible());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());

        contentCategoryMapper.insert(entity);
        log.info("创建内容分类成功: id={}, categoryName={}", entity.getId(), dto.getCategoryName());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ContentCategoryUpdateDTO dto) {
        ContentCategory existing = requireById(id);
        ContentCategory update = new ContentCategory();
        update.setId(existing.getId());

        if (dto.getCategoryName() != null) update.setCategoryName(dto.getCategoryName());
        if (dto.getParentCode() != null) update.setParentCode(dto.getParentCode());
        if (dto.getCategoryType() != null) update.setCategoryType(dto.getCategoryType());
        if (dto.getIcon() != null) update.setIcon(dto.getIcon());
        if (dto.getCoverImage() != null) update.setCoverImage(dto.getCoverImage());
        if (dto.getDescription() != null) update.setDescription(dto.getDescription());
        if (dto.getContentCount() != null) update.setContentCount(dto.getContentCount());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getIsVisible() != null) update.setIsVisible(dto.getIsVisible());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());

        contentCategoryMapper.updateById(update);
        log.info("更新内容分类成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireById(id);
        contentCategoryMapper.deleteById(id);
        log.info("删除内容分类成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ContentCategory> buildWrapper(ContentCategoryQueryDTO query) {
        return new LambdaQueryWrapper<ContentCategory>()
                .eq(query.getCategoryCode() != null && !query.getCategoryCode().isEmpty(),
                        ContentCategory::getCategoryCode, query.getCategoryCode())
                .like(query.getCategoryName() != null && !query.getCategoryName().isEmpty(),
                        ContentCategory::getCategoryName, query.getCategoryName())
                .eq(query.getParentCode() != null && !query.getParentCode().isEmpty(),
                        ContentCategory::getParentCode, query.getParentCode())
                .eq(query.getCategoryType() != null,
                        ContentCategory::getCategoryType, query.getCategoryType())
                .eq(query.getStatus() != null, ContentCategory::getStatus, query.getStatus())
                .eq(query.getIsVisible() != null, ContentCategory::getIsVisible, query.getIsVisible())
                .orderByAsc(ContentCategory::getSortOrder)
                .orderByAsc(ContentCategory::getCategoryName)
                .orderByAsc(ContentCategory::getId);
    }

    private ContentCategory requireById(Long id) {
        ContentCategory entity = contentCategoryMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "内容分类不存在: id=" + id);
        }
        return entity;
    }

    private ContentCategoryVO toVO(ContentCategory entity) {
        ContentCategoryVO vo = new ContentCategoryVO();
        vo.setId(entity.getId());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setCategoryName(entity.getCategoryName());
        vo.setParentCode(entity.getParentCode());
        vo.setCategoryType(entity.getCategoryType());
        vo.setIcon(entity.getIcon());
        vo.setCoverImage(entity.getCoverImage());
        vo.setDescription(entity.getDescription());
        vo.setContentCount(entity.getContentCount());
        vo.setSortOrder(entity.getSortOrder());
        vo.setIsVisible(entity.getIsVisible());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
