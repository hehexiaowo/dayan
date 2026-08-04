package com.dayan.equity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.equity.dto.EquityBatchCreateDTO;
import com.dayan.equity.dto.EquityBatchQueryDTO;
import com.dayan.equity.dto.EquityBatchUpdateDTO;
import com.dayan.equity.entity.EquityBatch;
import com.dayan.equity.entity.EquityTemplate;
import com.dayan.equity.mapper.EquityBatchMapper;
import com.dayan.equity.service.EquityBatchService;
import com.dayan.equity.service.EquityTemplateService;
import com.dayan.equity.vo.EquityBatchVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权益批次服务实现。
 *
 * <p>{@code batch_code} = {@code "BC" + String.format("%08d", sequenceProvider.next("code:seq:BC:0"))}。
 *
 * <p>统计字段联动通过 {@link LambdaUpdateWrapper#setSql(String)} 拼接增量 SQL
 * （{@code SET xxx_count = xxx_count + N}），避免读-改-写竞态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquityBatchServiceImpl implements EquityBatchService {

    private static final String CODE_PREFIX = "BC";
    private static final String SEQ_KEY = "code:seq:BC:0";
    private static final int SEQ_WIDTH = 8;

    /** 批次状态：0=待生产 / 1=生产中 / 2=已完成 / 3=已出库 / 4=已关闭 */
    private static final int BATCH_STATUS_PENDING = 0;
    private static final int BATCH_STATUS_PRODUCING = 1;
    private static final int BATCH_STATUS_COMPLETED = 2;
    /** 模板启用状态 */
    private static final int TEMPLATE_STATUS_ENABLED = 1;

    private final EquityBatchMapper batchMapper;
    private final SequenceProvider sequenceProvider;
    /** 模板服务，用于校验模板存在且启用。 */
    private final EquityTemplateService templateService;

    @Override
    public PageResult<EquityBatchVO> page(EquityBatchQueryDTO query) {
        LambdaQueryWrapper<EquityBatch> wrapper = buildQueryWrapper(query);
        Page<EquityBatch> page = batchMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<EquityBatchVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<EquityBatchVO> list(EquityBatchQueryDTO query) {
        return batchMapper.selectList(buildQueryWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public EquityBatchVO getDetail(String batchCode) {
        return toVO(requireBatch(batchCode));
    }

    @Override
    public EquityBatch requireBatch(String batchCode) {
        EquityBatch batch = batchMapper.selectOne(new LambdaQueryWrapper<EquityBatch>()
                .eq(EquityBatch::getBatchCode, batchCode)
                .last("LIMIT 1"));
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权益批次不存在: " + batchCode);
        }
        return batch;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(EquityBatchCreateDTO dto) {
        // 校验模板存在且启用
        EquityTemplate template = templateService.requireTemplate(dto.getTemplateCode());
        if (template.getStatus() == null || template.getStatus() != TEMPLATE_STATUS_ENABLED) {
            throw new BusinessException(ErrorCode.BUSINESS, "权益模板未启用，无法创建批次: " + dto.getTemplateCode());
        }

        // 日期校验
        validateDateRange(dto.getProduceDate(), dto.getExpireDate());

        String batchCode = generateCode();

        EquityBatch entity = new EquityBatch();
        entity.setBatchCode(batchCode);
        entity.setBatchName(dto.getBatchName());
        entity.setTemplateCode(dto.getTemplateCode());
        entity.setChannelCode(dto.getChannelCode());
        entity.setTotalQuantity(dto.getTotalQuantity());
        entity.setProducedCount(0);
        entity.setAllocatedCount(0);
        entity.setOutboundCount(0);
        entity.setActivatedCount(0);
        entity.setUsedCount(0);
        entity.setExpiredCount(0);
        entity.setVoidedCount(0);
        entity.setRemainCount(0);
        entity.setUnitCost(dto.getUnitCost());
        entity.setTotalCost(dto.getTotalCost());
        entity.setProduceDate(dto.getProduceDate());
        entity.setExpireDate(dto.getExpireDate());
        entity.setBatchStatus(dto.getBatchStatus() == null ? BATCH_STATUS_PENDING : dto.getBatchStatus());
        entity.setRemark(dto.getRemark());

        batchMapper.insert(entity);
        log.info("创建权益批次成功: batchCode={}, templateCode={}", batchCode, dto.getTemplateCode());
        return batchCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String batchCode, EquityBatchUpdateDTO dto) {
        EquityBatch existing = requireBatch(batchCode);
        EquityBatch update = new EquityBatch();
        update.setId(existing.getId());

        if (dto.getBatchName() != null) update.setBatchName(dto.getBatchName());
        if (dto.getChannelCode() != null) update.setChannelCode(dto.getChannelCode());
        if (dto.getUnitCost() != null) update.setUnitCost(dto.getUnitCost());
        if (dto.getTotalCost() != null) update.setTotalCost(dto.getTotalCost());
        if (dto.getProduceDate() != null || dto.getExpireDate() != null) {
            LocalDate produce = dto.getProduceDate() != null ? dto.getProduceDate() : existing.getProduceDate();
            LocalDate expire = dto.getExpireDate() != null ? dto.getExpireDate() : existing.getExpireDate();
            validateDateRange(produce, expire);
            if (dto.getProduceDate() != null) update.setProduceDate(dto.getProduceDate());
            if (dto.getExpireDate() != null) update.setExpireDate(dto.getExpireDate());
        }
        if (dto.getBatchStatus() != null) update.setBatchStatus(dto.getBatchStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        batchMapper.updateById(update);
        log.info("更新权益批次成功: batchCode={}", batchCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String batchCode) {
        EquityBatch existing = requireBatch(batchCode);
        if (existing.getProducedCount() != null && existing.getProducedCount() > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "批次已生产权益，无法删除: " + batchCode);
        }
        batchMapper.deleteById(existing.getId());
        log.info("删除权益批次成功: batchCode={}", batchCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementStat(String batchCode, String column, int delta) {
        if (delta == 0) {
            return;
        }
        // 列名做白名单校验，避免 SQL 注入
        if (!isAllowedColumn(column)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法的批次统计字段: " + column);
        }
        int rows = batchMapper.update(null, new LambdaUpdateWrapper<EquityBatch>()
                .eq(EquityBatch::getBatchCode, batchCode)
                .setSql(column + " = " + column + " + " + delta));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "批次统计联动失败（批次不存在）: " + batchCode);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBatchStatus(String batchCode, int newStatus) {
        int rows = batchMapper.update(null, new LambdaUpdateWrapper<EquityBatch>()
                .eq(EquityBatch::getBatchCode, batchCode)
                .set(EquityBatch::getBatchStatus, newStatus));
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "批次状态更新失败（批次不存在）: " + batchCode);
        }
    }

    // ====== 内部方法 ======

    /**
     * 批次统计字段白名单（数据库列名）。
     */
    private boolean isAllowedColumn(String column) {
        return "produced_count".equals(column)
                || "allocated_count".equals(column)
                || "outbound_count".equals(column)
                || "activated_count".equals(column)
                || "used_count".equals(column)
                || "expired_count".equals(column)
                || "voided_count".equals(column)
                || "remain_count".equals(column);
    }

    private String generateCode() {
        long seq = sequenceProvider.next(SEQ_KEY);
        return CODE_PREFIX + String.format("%0" + SEQ_WIDTH + "d", seq);
    }

    private void validateDateRange(LocalDate produce, LocalDate expire) {
        if (produce != null && expire != null && !expire.isAfter(produce)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "批次有效期必须晚于生产日期");
        }
    }

    private LambdaQueryWrapper<EquityBatch> buildQueryWrapper(EquityBatchQueryDTO query) {
        LambdaQueryWrapper<EquityBatch> wrapper = new LambdaQueryWrapper<EquityBatch>()
                .orderByDesc(EquityBatch::getCreatedAt);
        if (query.getBatchCode() != null && !query.getBatchCode().isEmpty()) {
            wrapper.eq(EquityBatch::getBatchCode, query.getBatchCode());
        }
        if (query.getBatchName() != null && !query.getBatchName().isEmpty()) {
            wrapper.like(EquityBatch::getBatchName, query.getBatchName());
        }
        if (query.getTemplateCode() != null && !query.getTemplateCode().isEmpty()) {
            wrapper.eq(EquityBatch::getTemplateCode, query.getTemplateCode());
        }
        if (query.getChannelCode() != null && !query.getChannelCode().isEmpty()) {
            wrapper.eq(EquityBatch::getChannelCode, query.getChannelCode());
        }
        if (query.getBatchStatus() != null) {
            wrapper.eq(EquityBatch::getBatchStatus, query.getBatchStatus());
        }
        return wrapper;
    }

    private EquityBatchVO toVO(EquityBatch entity) {
        EquityBatchVO vo = new EquityBatchVO();
        vo.setId(entity.getId());
        vo.setBatchCode(entity.getBatchCode());
        vo.setBatchName(entity.getBatchName());
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setChannelCode(entity.getChannelCode());
        vo.setTotalQuantity(entity.getTotalQuantity());
        vo.setProducedCount(entity.getProducedCount());
        vo.setAllocatedCount(entity.getAllocatedCount());
        vo.setOutboundCount(entity.getOutboundCount());
        vo.setActivatedCount(entity.getActivatedCount());
        vo.setUsedCount(entity.getUsedCount());
        vo.setExpiredCount(entity.getExpiredCount());
        vo.setVoidedCount(entity.getVoidedCount());
        vo.setRemainCount(entity.getRemainCount());
        vo.setUnitCost(entity.getUnitCost());
        vo.setTotalCost(entity.getTotalCost());
        vo.setProduceDate(entity.getProduceDate());
        vo.setExpireDate(entity.getExpireDate());
        vo.setBatchStatus(entity.getBatchStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
