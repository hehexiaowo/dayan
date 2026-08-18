package com.dayan.tool.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.tool.dto.ToolAiCreatorCreateDTO;
import com.dayan.tool.dto.ToolAiCreatorQueryDTO;
import com.dayan.tool.entity.ToolAiCreator;
import com.dayan.tool.mapper.ToolAiCreatorMapper;
import com.dayan.tool.model.AiPurpose;
import com.dayan.tool.model.ToolAiCreatorPhase;
import com.dayan.tool.service.ToolAiCreatorService;
import com.dayan.tool.vo.AiFactDigestVO;
import com.dayan.tool.vo.AiAuditItemVO;
import com.dayan.tool.vo.AiImageVO;
import com.dayan.tool.vo.AiOutlineVO;
import com.dayan.tool.vo.AiScoresVO;
import com.dayan.tool.vo.AiStrategyVO;
import com.dayan.tool.vo.AiTitleVO;
import com.dayan.tool.vo.ToolAiCreatorListVO;
import com.dayan.tool.vo.ToolAiCreatorRefsVO;
import com.dayan.tool.vo.ToolAiCreatorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 创作项目 CRUD 实现（tool 域）。
 *
 * <p>素材为前端聚合快照：material_refs 存引用（带展示名），materials 存素材块，
 * digest 之后流水线不再回查业务库。channelCode 走租户拦截器，agentCode 归属过滤。
 */
@Service
@RequiredArgsConstructor
public class ToolAiCreatorServiceImpl implements ToolAiCreatorService {

    private final ToolAiCreatorMapper mapper;

    @Override
    public Long create(ToolAiCreatorCreateDTO dto) {
        String agentCode = requireAgentCode();
        AiPurpose.requireValid(dto.getPurpose());
        // 目的 → 必填素材校验（空素材跑流水线只会产出幻觉）
        ToolAiCreatorRefsVO refs = dto.getMaterialRefs() == null ? new ToolAiCreatorRefsVO() : dto.getMaterialRefs();
        switch (dto.getPurpose()) {
            case AiPurpose.PRODUCT -> {
                if (refs.getKbFiles() == null || refs.getKbFiles().isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "产品宣传需在渠道知识库勾选保险产品/政策资料");
                }
                if (refs.getGoods() == null || refs.getGoods().isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "产品宣传需选择要推荐的权益商品");
                }
            }
            case AiPurpose.PARK -> {
                if (refs.getParks() == null || refs.getParks().isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "机构推荐需选择养老机构");
                }
            }
            case AiPurpose.SCIENCE -> {
                if (StrUtil.isBlank(dto.getTopic())) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "科普获客需填写主题/切入话题");
                }
            }
            default -> AiPurpose.requireValid(dto.getPurpose());
        }
        ToolAiCreator entity = new ToolAiCreator();
        entity.setAgentCode(agentCode);
        entity.setChannelCode(StrUtil.nullToEmpty(ContextHolder.getChannelCode()));
        entity.setPurpose(dto.getPurpose());
        entity.setContentType(dto.getContentType());
        entity.setStyleCode(dto.getStyleCode());
        entity.setAudience(StrUtil.blankToDefault(dto.getAudience(), "general"));
        entity.setTopic(dto.getTopic());
        entity.setMaterialRefs(JSONUtil.toJsonStr(refs));
        if (dto.getMaterials() != null && !dto.getMaterials().isEmpty()) {
            entity.setMaterials(JSONUtil.toJsonStr(dto.getMaterials()));
        }
        entity.setStatus(ToolAiCreatorPhase.CREATED);
        mapper.insert(entity);
        return entity.getId();
    }

    @Override
    public ToolAiCreatorVO getDetail(Long id) {
        return toVO(requireOwned(id));
    }

    @Override
    public PageResult<ToolAiCreatorListVO> page(ToolAiCreatorQueryDTO dto) {
        String agentCode = requireAgentCode();
        LambdaQueryWrapper<ToolAiCreator> wrapper = new LambdaQueryWrapper<ToolAiCreator>()
                .eq(ToolAiCreator::getAgentCode, agentCode)
                .eq(StrUtil.isNotBlank(dto.getStatus()), ToolAiCreator::getStatus, dto.getStatus())
                .orderByDesc(ToolAiCreator::getId);
        Page<ToolAiCreator> page = mapper.selectPage(new Page<>(dto.getCurrent(), dto.getSize()), wrapper);
        List<ToolAiCreatorListVO> records = page.getRecords().stream().map(e -> {
            ToolAiCreatorListVO vo = new ToolAiCreatorListVO();
            vo.setId(e.getId());
            vo.setPurpose(e.getPurpose());
            vo.setContentType(e.getContentType());
            vo.setTopic(e.getTopic());
            vo.setSelectedTitle(e.getSelectedTitle());
            vo.setStatus(e.getStatus());
            vo.setUpdatedAt(e.getUpdatedAt());
            return vo;
        }).toList();
        return new PageResult<>(dto.getCurrent(), dto.getSize(), page.getTotal(), records);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(requireOwned(id).getId());
    }

    @Override
    public ToolAiCreator requireOwned(Long id) {
        ToolAiCreator entity = mapper.selectById(id);
        if (entity == null || !requireAgentCode().equals(entity.getAgentCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "创作项目不存在");
        }
        return entity;
    }

    @Override
    public int updateById(ToolAiCreator entity) {
        return mapper.updateById(entity);
    }

    @Override
    public ToolAiCreatorVO toVO(ToolAiCreator e) {
        ToolAiCreatorVO vo = new ToolAiCreatorVO();
        vo.setId(e.getId());
        vo.setPurpose(e.getPurpose());
        vo.setContentType(e.getContentType());
        vo.setStyleCode(e.getStyleCode());
        vo.setAudience(e.getAudience());
        vo.setTopic(e.getTopic());
        vo.setStatus(e.getStatus());
        vo.setSelectedTitle(e.getSelectedTitle());
        vo.setBody(e.getBody());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        ToolAiCreatorRefsVO refs = parse(e.getMaterialRefs(), ToolAiCreatorRefsVO.class);
        vo.setMaterialRefs(refs);
        vo.setFactDigest(parse(e.getFactDigest(), AiFactDigestVO.class));
        vo.setStrategy(parse(e.getStrategy(), AiStrategyVO.class));
        vo.setTitles(parseList(e.getTitles(), AiTitleVO.class));
        vo.setOutline(parse(e.getOutline(), AiOutlineVO.class));
        vo.setScores(parse(e.getScores(), AiScoresVO.class));
        vo.setImages(parseList(e.getImages(), AiImageVO.class));
        vo.setAuditLog(parseList(e.getAuditLog(), AiAuditItemVO.class));
        vo.setWarnings(parseList(e.getWarnings(), String.class));
        resolveRefNames(vo, refs);
        return vo;
    }

    /** 素材名回显：refs 带展示名直读（旧数据字段为 null 时安全降级为空） */
    private void resolveRefNames(ToolAiCreatorVO vo, ToolAiCreatorRefsVO refs) {
        if (refs == null) {
            return;
        }
        if (StrUtil.isNotBlank(refs.getRefContentCode())) {
            vo.setRefContentName(refs.getRefContentCode());
        }
        if (refs.getKbFiles() != null) {
            vo.setKbFileNames(refs.getKbFiles().stream()
                    .map(ToolAiCreatorRefsVO.KbFileRef::getFileName).toList());
        }
        if (refs.getGoods() != null) {
            vo.setGoodsNames(refs.getGoods().stream()
                    .map(ToolAiCreatorRefsVO.CodeNameRef::getName).toList());
        }
        if (refs.getParks() != null) {
            vo.setParkNames(refs.getParks().stream()
                    .map(ToolAiCreatorRefsVO.CodeNameRef::getName).toList());
        }
    }

    private <T> T parse(String json, Class<T> clazz) {
        return StrUtil.isBlank(json) ? null : JSONUtil.toBean(json, clazz);
    }

    private <T> List<T> parseList(String json, Class<T> clazz) {
        return StrUtil.isBlank(json) ? null : JSONUtil.toList(JSONUtil.parseArray(json), clazz);
    }

    private String requireAgentCode() {
        String agentCode = ContextHolder.getAccountCode();
        if (StrUtil.isBlank(agentCode)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return agentCode;
    }
}
