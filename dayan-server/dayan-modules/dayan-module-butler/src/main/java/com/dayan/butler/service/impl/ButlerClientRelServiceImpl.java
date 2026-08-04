package com.dayan.butler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.butler.dto.ButlerClientRelBindDTO;
import com.dayan.butler.dto.ButlerClientRelQueryDTO;
import com.dayan.butler.entity.ButlerClientRel;
import com.dayan.butler.mapper.ButlerClientRelMapper;
import com.dayan.butler.service.ButlerClientRelService;
import com.dayan.butler.vo.ButlerClientRelVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管家-客户绑定关系服务实现。
 *
 * <p><b>一客户一管家约束</b>：bind 时查同 clientCode 是否已有 status=1 的有效绑定，
 * 有则抛 BusinessException("该客户已绑定管家，请先解绑")。
 * 解绑（unbind）= 将对应记录 status 置 0。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ButlerClientRelServiceImpl implements ButlerClientRelService {

    /** 有效状态 */
    private static final int STATUS_ACTIVE = 1;
    /** 已解绑状态 */
    private static final int STATUS_INACTIVE = 0;

    private final ButlerClientRelMapper butlerClientRelMapper;

    @Override
    public PageResult<ButlerClientRelVO> page(ButlerClientRelQueryDTO query) {
        LambdaQueryWrapper<ButlerClientRel> wrapper = buildQueryWrapper(query);
        Page<ButlerClientRel> page = butlerClientRelMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ButlerClientRelVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ButlerClientRelVO> list(ButlerClientRelQueryDTO query) {
        LambdaQueryWrapper<ButlerClientRel> wrapper = buildQueryWrapper(query);
        return butlerClientRelMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public ButlerClientRelVO getDetail(Long id) {
        return toVO(requireRel(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long bind(ButlerClientRelBindDTO dto) {
        String clientCode = dto.getClientCode();
        String butlerCode = dto.getButlerCode();

        // 一客户一管家约束：同 clientCode 仅允许 1 条 status=1 的有效绑定
        Long activeCount = butlerClientRelMapper.selectCount(new LambdaQueryWrapper<ButlerClientRel>()
                .eq(ButlerClientRel::getClientCode, clientCode)
                .eq(ButlerClientRel::getStatus, STATUS_ACTIVE));
        if (activeCount != null && activeCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "该客户已绑定管家，请先解绑");
        }

        ButlerClientRel entity = new ButlerClientRel();
        entity.setButlerCode(butlerCode);
        entity.setClientCode(clientCode);
        entity.setBindTime(LocalDateTime.now());
        entity.setStatus(STATUS_ACTIVE);
        butlerClientRelMapper.insert(entity);
        log.info("绑定管家-客户成功: id={}, butlerCode={}, clientCode={}",
                entity.getId(), butlerCode, clientCode);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long id) {
        ButlerClientRel existing = requireRel(id);
        ButlerClientRel update = new ButlerClientRel();
        update.setId(existing.getId());
        update.setStatus(STATUS_INACTIVE);
        butlerClientRelMapper.updateById(update);
        log.info("解绑管家-客户: id={}, clientCode={}", id, existing.getClientCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ButlerClientRel existing = requireRel(id);
        butlerClientRelMapper.deleteById(existing.getId());
        log.info("删除管家-客户绑定: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ButlerClientRel> buildQueryWrapper(ButlerClientRelQueryDTO query) {
        return new LambdaQueryWrapper<ButlerClientRel>()
                .eq(query.getButlerCode() != null && !query.getButlerCode().isEmpty(),
                        ButlerClientRel::getButlerCode, query.getButlerCode())
                .eq(query.getClientCode() != null && !query.getClientCode().isEmpty(),
                        ButlerClientRel::getClientCode, query.getClientCode())
                .eq(query.getStatus() != null, ButlerClientRel::getStatus, query.getStatus())
                .orderByDesc(ButlerClientRel::getCreatedAt);
    }

    private ButlerClientRel requireRel(Long id) {
        ButlerClientRel rel = butlerClientRelMapper.selectById(id);
        if (rel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "管家-客户绑定不存在: " + id);
        }
        return rel;
    }

    private ButlerClientRelVO toVO(ButlerClientRel entity) {
        ButlerClientRelVO vo = new ButlerClientRelVO();
        vo.setId(entity.getId());
        vo.setButlerCode(entity.getButlerCode());
        vo.setClientCode(entity.getClientCode());
        vo.setBindTime(entity.getBindTime());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
