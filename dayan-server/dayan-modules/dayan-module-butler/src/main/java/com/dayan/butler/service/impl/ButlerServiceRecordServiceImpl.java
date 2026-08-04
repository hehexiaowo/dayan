package com.dayan.butler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.butler.dto.ButlerServiceRecordCreateDTO;
import com.dayan.butler.dto.ButlerServiceRecordQueryDTO;
import com.dayan.butler.dto.ButlerServiceRecordUpdateDTO;
import com.dayan.butler.entity.ButlerServiceRecord;
import com.dayan.butler.mapper.ButlerServiceRecordMapper;
import com.dayan.butler.service.ButlerServiceRecordService;
import com.dayan.butler.vo.ButlerServiceRecordVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管家服务记录服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ButlerServiceRecordServiceImpl implements ButlerServiceRecordService {

    /** 默认状态 */
    private static final int DEFAULT_STATUS = 1;

    private final ButlerServiceRecordMapper butlerServiceRecordMapper;

    @Override
    public PageResult<ButlerServiceRecordVO> page(ButlerServiceRecordQueryDTO query) {
        LambdaQueryWrapper<ButlerServiceRecord> wrapper = buildQueryWrapper(query);
        Page<ButlerServiceRecord> page = butlerServiceRecordMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ButlerServiceRecordVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ButlerServiceRecordVO> list(ButlerServiceRecordQueryDTO query) {
        LambdaQueryWrapper<ButlerServiceRecord> wrapper = buildQueryWrapper(query);
        return butlerServiceRecordMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public ButlerServiceRecordVO getDetail(Long id) {
        return toVO(requireRecord(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ButlerServiceRecordCreateDTO dto) {
        ButlerServiceRecord entity = new ButlerServiceRecord();
        entity.setButlerCode(dto.getButlerCode());
        entity.setClientCode(dto.getClientCode());
        entity.setServiceType(dto.getServiceType());
        entity.setServiceTitle(dto.getServiceTitle());
        entity.setServiceDate(dto.getServiceDate());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());
        entity.setCommunicateWay(dto.getCommunicateWay());
        entity.setRemark(dto.getRemark());

        butlerServiceRecordMapper.insert(entity);
        log.info("创建管家服务记录成功: id={}, butlerCode={}, clientCode={}",
                entity.getId(), dto.getButlerCode(), dto.getClientCode());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ButlerServiceRecordUpdateDTO dto) {
        ButlerServiceRecord existing = requireRecord(id);
        ButlerServiceRecord update = new ButlerServiceRecord();
        update.setId(existing.getId());

        if (dto.getServiceType() != null) update.setServiceType(dto.getServiceType());
        if (dto.getServiceTitle() != null) update.setServiceTitle(dto.getServiceTitle());
        if (dto.getServiceDate() != null) update.setServiceDate(dto.getServiceDate());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getCommunicateWay() != null) update.setCommunicateWay(dto.getCommunicateWay());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        butlerServiceRecordMapper.updateById(update);
        log.info("更新管家服务记录成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ButlerServiceRecord existing = requireRecord(id);
        butlerServiceRecordMapper.deleteById(existing.getId());
        log.info("删除管家服务记录成功: id={}", id);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ButlerServiceRecord> buildQueryWrapper(ButlerServiceRecordQueryDTO query) {
        return new LambdaQueryWrapper<ButlerServiceRecord>()
                .eq(query.getButlerCode() != null && !query.getButlerCode().isEmpty(),
                        ButlerServiceRecord::getButlerCode, query.getButlerCode())
                .eq(query.getClientCode() != null && !query.getClientCode().isEmpty(),
                        ButlerServiceRecord::getClientCode, query.getClientCode())
                .eq(query.getServiceType() != null,
                        ButlerServiceRecord::getServiceType, query.getServiceType())
                .eq(query.getCommunicateWay() != null,
                        ButlerServiceRecord::getCommunicateWay, query.getCommunicateWay())
                .eq(query.getStatus() != null,
                        ButlerServiceRecord::getStatus, query.getStatus())
                .orderByDesc(ButlerServiceRecord::getCreatedAt);
    }

    private ButlerServiceRecord requireRecord(Long id) {
        ButlerServiceRecord record = butlerServiceRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "管家服务记录不存在: " + id);
        }
        return record;
    }

    private ButlerServiceRecordVO toVO(ButlerServiceRecord entity) {
        ButlerServiceRecordVO vo = new ButlerServiceRecordVO();
        vo.setId(entity.getId());
        vo.setButlerCode(entity.getButlerCode());
        vo.setClientCode(entity.getClientCode());
        vo.setServiceType(entity.getServiceType());
        vo.setServiceTitle(entity.getServiceTitle());
        vo.setServiceDate(entity.getServiceDate());
        vo.setStatus(entity.getStatus());
        vo.setCommunicateWay(entity.getCommunicateWay());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
