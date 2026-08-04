package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.park.dto.ParkAdviserCreateDTO;
import com.dayan.park.dto.ParkAdviserQueryDTO;
import com.dayan.park.dto.ParkAdviserUpdateDTO;
import com.dayan.park.entity.ParkAdviser;
import com.dayan.park.mapper.ParkAdviserMapper;
import com.dayan.park.service.ParkAdviserService;
import com.dayan.park.vo.ParkAdviserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构顾问（park_adviser）服务实现。
 *
 * <p>主顾问唯一约束：同一 parkCode 下 isPrimary=1 仅允许 1 个；设主时自动将同 parkCode
 * 其他顾问的 isPrimary 置 0。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkAdviserServiceImpl implements ParkAdviserService {

    /** isPrimary 标记：1=主顾问 */
    private static final int IS_PRIMARY_YES = 1;

    private final ParkAdviserMapper adviserMapper;

    @Override
    public PageResult<ParkAdviserVO> page(ParkAdviserQueryDTO query) {
        LambdaQueryWrapper<ParkAdviser> wrapper = buildWrapper(query);
        Page<ParkAdviser> page = adviserMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ParkAdviserVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ParkAdviserVO> listByPark(String parkCode) {
        return adviserMapper.selectList(new LambdaQueryWrapper<ParkAdviser>()
                .eq(ParkAdviser::getParkCode, parkCode)
                .orderByDesc(ParkAdviser::getIsPrimary)
                .orderByAsc(ParkAdviser::getSortOrder)
                .orderByAsc(ParkAdviser::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ParkAdviserVO getDetail(Long id) {
        return toVO(requireAdviser(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ParkAdviserCreateDTO dto) {
        Integer isPrimary = (dto.getIsPrimary() != null && dto.getIsPrimary() == IS_PRIMARY_YES)
                ? IS_PRIMARY_YES : 0;
        if (isPrimary == IS_PRIMARY_YES) {
            clearOtherPrimary(dto.getParkCode(), null);
        }

        ParkAdviser entity = new ParkAdviser();
        entity.setParkCode(dto.getParkCode());
        entity.setAdviserName(dto.getAdviserName());
        entity.setAdviserTitle(dto.getAdviserTitle());
        entity.setAdviserImage(dto.getAdviserImage());
        entity.setAdviserContent(dto.getAdviserContent());
        entity.setContactPhone(dto.getContactPhone());
        entity.setIsPrimary(isPrimary);
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        adviserMapper.insert(entity);
        log.info("创建机构顾问成功: parkCode={}, id={}, isPrimary={}",
                dto.getParkCode(), entity.getId(), isPrimary);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ParkAdviserUpdateDTO dto) {
        ParkAdviser existing = requireAdviser(id);
        ParkAdviser update = new ParkAdviser();
        update.setId(existing.getId());
        if (dto.getAdviserName() != null) update.setAdviserName(dto.getAdviserName());
        if (dto.getAdviserTitle() != null) update.setAdviserTitle(dto.getAdviserTitle());
        if (dto.getAdviserImage() != null) update.setAdviserImage(dto.getAdviserImage());
        if (dto.getAdviserContent() != null) update.setAdviserContent(dto.getAdviserContent());
        if (dto.getContactPhone() != null) update.setContactPhone(dto.getContactPhone());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getIsPrimary() != null) {
            int isPrimary = (dto.getIsPrimary() == IS_PRIMARY_YES) ? IS_PRIMARY_YES : 0;
            if (isPrimary == IS_PRIMARY_YES) {
                clearOtherPrimary(existing.getParkCode(), id);
            }
            update.setIsPrimary(isPrimary);
        }
        adviserMapper.updateById(update);
        log.info("更新机构顾问成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParkAdviser existing = requireAdviser(id);
        adviserMapper.deleteById(existing.getId());
        log.info("删除机构顾问成功: id={}", id);
    }

    // ====== 内部方法 ======

    /** 清除同 parkCode 下其他顾问的主顾问标记（排除 excludeId） */
    private void clearOtherPrimary(String parkCode, Long excludeId) {
        LambdaUpdateWrapper<ParkAdviser> wrapper = new LambdaUpdateWrapper<ParkAdviser>()
                .eq(ParkAdviser::getParkCode, parkCode)
                .eq(ParkAdviser::getIsPrimary, IS_PRIMARY_YES)
                .set(ParkAdviser::getIsPrimary, 0);
        if (excludeId != null) {
            wrapper.ne(ParkAdviser::getId, excludeId);
        }
        adviserMapper.update(null, wrapper);
    }

    private LambdaQueryWrapper<ParkAdviser> buildWrapper(ParkAdviserQueryDTO query) {
        LambdaQueryWrapper<ParkAdviser> wrapper = new LambdaQueryWrapper<ParkAdviser>()
                .orderByDesc(ParkAdviser::getIsPrimary)
                .orderByAsc(ParkAdviser::getSortOrder)
                .orderByAsc(ParkAdviser::getId);
        if (query.getParkCode() != null && !query.getParkCode().isEmpty()) {
            wrapper.eq(ParkAdviser::getParkCode, query.getParkCode());
        }
        if (query.getAdviserName() != null && !query.getAdviserName().isEmpty()) {
            wrapper.like(ParkAdviser::getAdviserName, query.getAdviserName());
        }
        if (query.getIsPrimary() != null) {
            wrapper.eq(ParkAdviser::getIsPrimary, query.getIsPrimary());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ParkAdviser::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private ParkAdviser requireAdviser(Long id) {
        ParkAdviser entity = adviserMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "机构顾问不存在: id=" + id);
        }
        return entity;
    }

    private ParkAdviserVO toVO(ParkAdviser entity) {
        ParkAdviserVO vo = new ParkAdviserVO();
        vo.setId(entity.getId());
        vo.setParkCode(entity.getParkCode());
        vo.setAdviserName(entity.getAdviserName());
        vo.setAdviserTitle(entity.getAdviserTitle());
        vo.setAdviserImage(entity.getAdviserImage());
        vo.setAdviserContent(entity.getAdviserContent());
        vo.setContactPhone(entity.getContactPhone());
        vo.setIsPrimary(entity.getIsPrimary());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
