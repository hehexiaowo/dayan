package com.dayan.butler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.butler.dto.ButlerSkillCreateDTO;
import com.dayan.butler.dto.ButlerSkillQueryDTO;
import com.dayan.butler.dto.ButlerSkillUpdateDTO;
import com.dayan.butler.entity.ButlerSkill;
import com.dayan.butler.mapper.ButlerSkillMapper;
import com.dayan.butler.service.ButlerSkillService;
import com.dayan.butler.vo.ButlerSkillVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管家技能服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ButlerSkillServiceImpl implements ButlerSkillService {

    private final ButlerSkillMapper butlerSkillMapper;

    @Override
    public PageResult<ButlerSkillVO> page(ButlerSkillQueryDTO query) {
        LambdaQueryWrapper<ButlerSkill> wrapper = buildQueryWrapper(query);
        Page<ButlerSkill> page = butlerSkillMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ButlerSkillVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ButlerSkillVO> list(ButlerSkillQueryDTO query) {
        LambdaQueryWrapper<ButlerSkill> wrapper = buildQueryWrapper(query);
        return butlerSkillMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public ButlerSkillVO getDetail(Long id) {
        return toVO(requireSkill(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ButlerSkillCreateDTO dto) {
        ButlerSkill entity = new ButlerSkill();
        entity.setButlerCode(dto.getButlerCode());
        entity.setSkillCode(dto.getSkillCode());
        entity.setSkillName(dto.getSkillName());
        entity.setProficiency(dto.getProficiency());
        entity.setIsCertified(dto.getIsCertified());
        entity.setCertificateNo(dto.getCertificateNo());
        entity.setObtainDate(dto.getObtainDate());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());

        butlerSkillMapper.insert(entity);
        log.info("创建管家技能成功: id={}, butlerCode={}, skillCode={}",
                entity.getId(), dto.getButlerCode(), dto.getSkillCode());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ButlerSkillUpdateDTO dto) {
        ButlerSkill existing = requireSkill(id);
        ButlerSkill update = new ButlerSkill();
        update.setId(existing.getId());

        if (dto.getSkillName() != null) update.setSkillName(dto.getSkillName());
        if (dto.getProficiency() != null) update.setProficiency(dto.getProficiency());
        if (dto.getIsCertified() != null) update.setIsCertified(dto.getIsCertified());
        if (dto.getCertificateNo() != null) update.setCertificateNo(dto.getCertificateNo());
        if (dto.getObtainDate() != null) update.setObtainDate(dto.getObtainDate());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());

        butlerSkillMapper.updateById(update);
        log.info("更新管家技能成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ButlerSkill existing = requireSkill(id);
        butlerSkillMapper.deleteById(existing.getId());
        log.info("删除管家技能成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ButlerSkill> buildQueryWrapper(ButlerSkillQueryDTO query) {
        return new LambdaQueryWrapper<ButlerSkill>()
                .eq(query.getButlerCode() != null && !query.getButlerCode().isEmpty(),
                        ButlerSkill::getButlerCode, query.getButlerCode())
                .eq(query.getSkillCode() != null && !query.getSkillCode().isEmpty(),
                        ButlerSkill::getSkillCode, query.getSkillCode())
                .like(query.getSkillName() != null && !query.getSkillName().isEmpty(),
                        ButlerSkill::getSkillName, query.getSkillName())
                .eq(query.getIsCertified() != null,
                        ButlerSkill::getIsCertified, query.getIsCertified())
                .orderByAsc(ButlerSkill::getSortOrder)
                .orderByDesc(ButlerSkill::getCreatedAt);
    }

    private ButlerSkill requireSkill(Long id) {
        ButlerSkill skill = butlerSkillMapper.selectById(id);
        if (skill == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "管家技能不存在: " + id);
        }
        return skill;
    }

    private ButlerSkillVO toVO(ButlerSkill entity) {
        ButlerSkillVO vo = new ButlerSkillVO();
        vo.setId(entity.getId());
        vo.setButlerCode(entity.getButlerCode());
        vo.setSkillCode(entity.getSkillCode());
        vo.setSkillName(entity.getSkillName());
        vo.setProficiency(entity.getProficiency());
        vo.setIsCertified(entity.getIsCertified());
        vo.setCertificateNo(entity.getCertificateNo());
        vo.setObtainDate(entity.getObtainDate());
        vo.setSortOrder(entity.getSortOrder());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
