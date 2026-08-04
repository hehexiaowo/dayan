package com.dayan.butler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.butler.dto.ButlerAccountRoleRelCreateDTO;
import com.dayan.butler.dto.ButlerAccountRoleRelQueryDTO;
import com.dayan.butler.dto.ButlerAccountRoleRelUpdateDTO;
import com.dayan.butler.entity.ButlerAccountRoleRel;
import com.dayan.butler.mapper.ButlerAccountRoleRelMapper;
import com.dayan.butler.service.ButlerAccountRoleRelService;
import com.dayan.butler.vo.ButlerAccountRoleRelVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管家账号-角色关联服务实现（P5 仅 CRUD 框架）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ButlerAccountRoleRelServiceImpl implements ButlerAccountRoleRelService {

    private final ButlerAccountRoleRelMapper butlerAccountRoleRelMapper;

    @Override
    public PageResult<ButlerAccountRoleRelVO> page(ButlerAccountRoleRelQueryDTO query) {
        LambdaQueryWrapper<ButlerAccountRoleRel> wrapper = buildQueryWrapper(query);
        Page<ButlerAccountRoleRel> page = butlerAccountRoleRelMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ButlerAccountRoleRelVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ButlerAccountRoleRelVO> list(ButlerAccountRoleRelQueryDTO query) {
        LambdaQueryWrapper<ButlerAccountRoleRel> wrapper = buildQueryWrapper(query);
        return butlerAccountRoleRelMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public ButlerAccountRoleRelVO getDetail(Long id) {
        return toVO(requireRel(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ButlerAccountRoleRelCreateDTO dto) {
        ButlerAccountRoleRel entity = new ButlerAccountRoleRel();
        entity.setAccountCode(dto.getAccountCode());
        entity.setButlerCode(dto.getButlerCode());
        entity.setRoleType(dto.getRoleType());
        entity.setDescription(dto.getDescription());

        butlerAccountRoleRelMapper.insert(entity);
        log.info("创建管家账号-角色关联成功: id={}, accountCode={}, butlerCode={}",
                entity.getId(), dto.getAccountCode(), dto.getButlerCode());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ButlerAccountRoleRelUpdateDTO dto) {
        ButlerAccountRoleRel existing = requireRel(id);
        ButlerAccountRoleRel update = new ButlerAccountRoleRel();
        update.setId(existing.getId());

        if (dto.getRoleType() != null) update.setRoleType(dto.getRoleType());
        if (dto.getDescription() != null) update.setDescription(dto.getDescription());

        butlerAccountRoleRelMapper.updateById(update);
        log.info("更新管家账号-角色关联成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ButlerAccountRoleRel existing = requireRel(id);
        butlerAccountRoleRelMapper.deleteById(existing.getId());
        log.info("删除管家账号-角色关联成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ButlerAccountRoleRel> buildQueryWrapper(ButlerAccountRoleRelQueryDTO query) {
        return new LambdaQueryWrapper<ButlerAccountRoleRel>()
                .eq(query.getAccountCode() != null && !query.getAccountCode().isEmpty(),
                        ButlerAccountRoleRel::getAccountCode, query.getAccountCode())
                .eq(query.getButlerCode() != null && !query.getButlerCode().isEmpty(),
                        ButlerAccountRoleRel::getButlerCode, query.getButlerCode())
                .eq(query.getRoleType() != null,
                        ButlerAccountRoleRel::getRoleType, query.getRoleType())
                .orderByDesc(ButlerAccountRoleRel::getCreatedAt);
    }

    private ButlerAccountRoleRel requireRel(Long id) {
        ButlerAccountRoleRel rel = butlerAccountRoleRelMapper.selectById(id);
        if (rel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "管家账号-角色关联不存在: " + id);
        }
        return rel;
    }

    private ButlerAccountRoleRelVO toVO(ButlerAccountRoleRel entity) {
        ButlerAccountRoleRelVO vo = new ButlerAccountRoleRelVO();
        vo.setId(entity.getId());
        vo.setAccountCode(entity.getAccountCode());
        vo.setButlerCode(entity.getButlerCode());
        vo.setRoleType(entity.getRoleType());
        vo.setDescription(entity.getDescription());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
