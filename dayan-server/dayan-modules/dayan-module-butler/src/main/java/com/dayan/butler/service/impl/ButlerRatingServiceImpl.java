package com.dayan.butler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.butler.dto.ButlerRatingCreateDTO;
import com.dayan.butler.dto.ButlerRatingQueryDTO;
import com.dayan.butler.dto.ButlerRatingUpdateDTO;
import com.dayan.butler.entity.ButlerRating;
import com.dayan.butler.mapper.ButlerRatingMapper;
import com.dayan.butler.service.ButlerRatingService;
import com.dayan.butler.vo.ButlerRatingVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管家评价服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ButlerRatingServiceImpl implements ButlerRatingService {

    /** 默认状态 */
    private static final int DEFAULT_STATUS = 1;

    private final ButlerRatingMapper butlerRatingMapper;

    @Override
    public PageResult<ButlerRatingVO> page(ButlerRatingQueryDTO query) {
        LambdaQueryWrapper<ButlerRating> wrapper = buildQueryWrapper(query);
        Page<ButlerRating> page = butlerRatingMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ButlerRatingVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ButlerRatingVO> list(ButlerRatingQueryDTO query) {
        LambdaQueryWrapper<ButlerRating> wrapper = buildQueryWrapper(query);
        return butlerRatingMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public ButlerRatingVO getDetail(Long id) {
        return toVO(requireRating(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ButlerRatingCreateDTO dto) {
        ButlerRating entity = new ButlerRating();
        entity.setButlerCode(dto.getButlerCode());
        entity.setClientCode(dto.getClientCode());
        entity.setServiceRecordCode(dto.getServiceRecordCode());
        entity.setRating(dto.getRating());
        entity.setContent(dto.getContent());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());

        butlerRatingMapper.insert(entity);
        log.info("创建管家评价成功: id={}, butlerCode={}, rating={}",
                entity.getId(), dto.getButlerCode(), dto.getRating());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ButlerRatingUpdateDTO dto) {
        ButlerRating existing = requireRating(id);
        ButlerRating update = new ButlerRating();
        update.setId(existing.getId());

        if (dto.getRating() != null) update.setRating(dto.getRating());
        if (dto.getContent() != null) update.setContent(dto.getContent());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());

        butlerRatingMapper.updateById(update);
        log.info("更新管家评价成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ButlerRating existing = requireRating(id);
        butlerRatingMapper.deleteById(existing.getId());
        log.info("删除管家评价成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ButlerRating> buildQueryWrapper(ButlerRatingQueryDTO query) {
        return new LambdaQueryWrapper<ButlerRating>()
                .eq(query.getButlerCode() != null && !query.getButlerCode().isEmpty(),
                        ButlerRating::getButlerCode, query.getButlerCode())
                .eq(query.getClientCode() != null && !query.getClientCode().isEmpty(),
                        ButlerRating::getClientCode, query.getClientCode())
                .eq(query.getServiceRecordCode() != null && !query.getServiceRecordCode().isEmpty(),
                        ButlerRating::getServiceRecordCode, query.getServiceRecordCode())
                .eq(query.getRating() != null, ButlerRating::getRating, query.getRating())
                .eq(query.getStatus() != null, ButlerRating::getStatus, query.getStatus())
                .orderByDesc(ButlerRating::getCreatedAt);
    }

    private ButlerRating requireRating(Long id) {
        ButlerRating rating = butlerRatingMapper.selectById(id);
        if (rating == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "管家评价不存在: " + id);
        }
        return rating;
    }

    private ButlerRatingVO toVO(ButlerRating entity) {
        ButlerRatingVO vo = new ButlerRatingVO();
        vo.setId(entity.getId());
        vo.setButlerCode(entity.getButlerCode());
        vo.setClientCode(entity.getClientCode());
        vo.setServiceRecordCode(entity.getServiceRecordCode());
        vo.setRating(entity.getRating());
        vo.setContent(entity.getContent());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
