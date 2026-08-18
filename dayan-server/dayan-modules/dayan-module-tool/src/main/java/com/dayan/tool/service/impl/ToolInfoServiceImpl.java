package com.dayan.tool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.tool.dto.ToolInfoCreateDTO;
import com.dayan.tool.dto.ToolInfoQueryDTO;
import com.dayan.tool.dto.ToolInfoUpdateDTO;
import com.dayan.tool.entity.ToolInfo;
import com.dayan.tool.mapper.ToolInfoMapper;
import com.dayan.tool.model.ToolType;
import com.dayan.tool.service.ToolInfoService;
import com.dayan.tool.vo.ToolInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 工具服务实现。
 *
 * <p>工具编码生成：{@code "TL" + String.format("%05d", sequenceProvider.next("code:seq:TL:0"))}，
 * 全表唯一。新建默认 status=1（启用）、visibleScope=agent。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolInfoServiceImpl implements ToolInfoService {

    /** 工具编码前缀 */
    private static final String CODE_PREFIX = "TL";
    /** 序列键 */
    private static final String SEQ_KEY = "code:seq:TL:0";
    /** 默认可见端 */
    private static final String DEFAULT_VISIBLE_SCOPE = "agent";
    /** 默认状态：启用 */
    private static final int DEFAULT_STATUS = 1;

    private final ToolInfoMapper toolInfoMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<ToolInfoVO> page(ToolInfoQueryDTO query) {
        LambdaQueryWrapper<ToolInfo> wrapper = buildQueryWrapper(query);
        Page<ToolInfo> page = toolInfoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ToolInfoVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ToolInfoVO> list(ToolInfoQueryDTO query) {
        return toolInfoMapper.selectList(buildQueryWrapper(query)).stream().map(this::toVO).toList();
    }

    @Override
    public ToolInfoVO getDetail(String toolCode) {
        return toVO(requireTool(toolCode));
    }

    @Override
    public List<ToolInfoVO> listForEnd(String end) {
        if (end == null || end.isEmpty()) {
            return List.of();
        }
        return toolInfoMapper.selectList(new LambdaQueryWrapper<ToolInfo>()
                        .eq(ToolInfo::getStatus, 1)
                        // visible_scope 逗号分隔多值，用 CONCAT 包裹边界防子串误匹配
                        .apply("CONCAT(',', `visible_scope`, ',') LIKE CONCAT('%,', {0}, ',%')", end)
                        .orderByAsc(ToolInfo::getSortOrder)
                        .orderByAsc(ToolInfo::getId))
                .stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ToolInfoCreateDTO dto) {
        String toolCode = generateToolCode();

        ToolType.requireValid(dto.getToolType());
        validateConfigJson(dto.getConfigJson());

        ToolInfo entity = new ToolInfo();
        entity.setToolCode(toolCode);
        entity.setToolName(dto.getToolName());
        entity.setToolType(dto.getToolType());
        entity.setToolDesc(dto.getToolDesc());
        entity.setIcon(dto.getIcon());
        entity.setEntryPath(dto.getEntryPath());
        entity.setConfigJson(dto.getConfigJson());
        entity.setVisibleScope(dto.getVisibleScope() == null || dto.getVisibleScope().isEmpty()
                ? DEFAULT_VISIBLE_SCOPE : dto.getVisibleScope());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());
        entity.setRemark(dto.getRemark());

        toolInfoMapper.insert(entity);
        log.info("创建工具成功: toolCode={}, toolName={}", toolCode, dto.getToolName());
        return toolCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String toolCode, ToolInfoUpdateDTO dto) {
        ToolInfo existing = requireTool(toolCode);
        ToolInfo update = new ToolInfo();
        update.setId(existing.getId());

        if (dto.getToolName() != null) update.setToolName(dto.getToolName());
        if (dto.getToolType() != null) {
            ToolType.requireValid(dto.getToolType());
            update.setToolType(dto.getToolType());
        }
        if (dto.getToolDesc() != null) update.setToolDesc(dto.getToolDesc());
        if (dto.getIcon() != null) update.setIcon(dto.getIcon());
        if (dto.getEntryPath() != null) update.setEntryPath(dto.getEntryPath());
        if (dto.getConfigJson() != null) {
            validateConfigJson(dto.getConfigJson());
            update.setConfigJson(dto.getConfigJson());
        }
        if (dto.getVisibleScope() != null) update.setVisibleScope(dto.getVisibleScope());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        toolInfoMapper.updateById(update);
        log.info("更新工具成功: toolCode={}", toolCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String toolCode) {
        ToolInfo existing = requireTool(toolCode);
        toolInfoMapper.deleteById(existing.getId());
        log.info("删除工具成功: toolCode={}", toolCode);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ToolInfo> buildQueryWrapper(ToolInfoQueryDTO query) {
        return new LambdaQueryWrapper<ToolInfo>()
                .eq(query.getToolCode() != null && !query.getToolCode().isEmpty(),
                        ToolInfo::getToolCode, query.getToolCode())
                .like(query.getToolName() != null && !query.getToolName().isEmpty(),
                        ToolInfo::getToolName, query.getToolName())
                .eq(query.getToolType() != null && !query.getToolType().isEmpty(),
                        ToolInfo::getToolType, query.getToolType())
                .eq(query.getStatus() != null, ToolInfo::getStatus, query.getStatus())
                .orderByAsc(ToolInfo::getSortOrder)
                .orderByDesc(ToolInfo::getCreatedAt);
    }

    private ToolInfo requireTool(String toolCode) {
        ToolInfo tool = toolInfoMapper.selectOne(new LambdaQueryWrapper<ToolInfo>()
                .eq(ToolInfo::getToolCode, toolCode)
                .last("LIMIT 1"));
        if (tool == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工具不存在: " + toolCode);
        }
        return tool;
    }

    /** 生成工具编码：TL + 5 位序列 */
    private String generateToolCode() {
        return CODE_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY));
    }

    private void validateConfigJson(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return;
        }
        try {
            cn.hutool.json.JSONUtil.parse(configJson);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工具配置不是合法 JSON");
        }
    }

    private ToolInfoVO toVO(ToolInfo entity) {
        ToolInfoVO vo = new ToolInfoVO();
        vo.setId(entity.getId());
        vo.setToolCode(entity.getToolCode());
        vo.setToolName(entity.getToolName());
        vo.setToolType(entity.getToolType());
        vo.setToolDesc(entity.getToolDesc());
        vo.setIcon(entity.getIcon());
        vo.setEntryPath(entity.getEntryPath());
        vo.setConfigJson(entity.getConfigJson());
        vo.setVisibleScope(entity.getVisibleScope());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
