package com.dayan.agent.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.agent.dto.AiProjectCreateDTO;
import com.dayan.agent.entity.AiCreationProject;
import com.dayan.agent.mapper.AiCreationProjectMapper;
import com.dayan.agent.model.AiProjectPhase;
import com.dayan.agent.model.AiPurpose;
import com.dayan.agent.service.AiCreationProjectService;
import com.dayan.agent.service.AiMaterialAssembler;
import com.dayan.agent.vo.*;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.mybatis.context.ContextHolder;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiCreationProjectServiceImpl implements AiCreationProjectService {

    private final AiCreationProjectMapper mapper;
    private final AiMaterialAssembler materialAssembler;

    @Override
    public Long create(AiProjectCreateDTO dto) {
        String agentCode = requireAgentCode();
        AiPurpose.requireValid(dto.getPurpose());
        // 目的 → 必填素材校验（空素材跑流水线只会产出幻觉）
        List<String> kb = dto.getKbFileIds();
        List<String> goods = dto.getGoodsCodes();
        List<String> parks = dto.getParkCodes();
        switch (dto.getPurpose()) {
            case AiPurpose.PRODUCT -> {
                if (kb == null || kb.isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "产品宣传需在渠道知识库勾选保险产品/政策资料");
                }
                if (goods == null || goods.isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "产品宣传需选择要推荐的权益商品");
                }
            }
            case AiPurpose.PARK -> {
                if (parks == null || parks.isEmpty()) {
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
        AiCreationProject entity = new AiCreationProject();
        entity.setAgentCode(agentCode);
        entity.setChannelCode(StrUtil.nullToEmpty(ContextHolder.getChannelCode()));
        entity.setPurpose(dto.getPurpose());
        entity.setContentType(dto.getContentType());
        entity.setStyleCode(dto.getStyleCode());
        entity.setAudience(StrUtil.blankToDefault(dto.getAudience(), "general"));
        entity.setTopic(dto.getTopic());
        AiMaterialRefsVO refs = new AiMaterialRefsVO();
        refs.setRefContentCode(dto.getRefContentCode());
        refs.setKbFileIds(kb);
        refs.setGoodsCodes(goods);
        refs.setParkCodes(parks);
        entity.setMaterialRefs(JSONUtil.toJsonStr(refs));
        entity.setStatus(AiProjectPhase.CREATED);
        mapper.insert(entity);
        return entity.getId();
    }

    @Override
    public AiProjectVO getDetail(Long id) {
        return toVO(requireOwned(id));
    }

    @Override
    public PageResult<AiProjectListVO> page(long current, long size, String status) {
        String agentCode = requireAgentCode();
        LambdaQueryWrapper<AiCreationProject> wrapper = new LambdaQueryWrapper<AiCreationProject>()
                .eq(AiCreationProject::getAgentCode, agentCode)
                .eq(StrUtil.isNotBlank(status), AiCreationProject::getStatus, status)
                .orderByDesc(AiCreationProject::getId);
        Page<AiCreationProject> page = mapper.selectPage(new Page<>(current, size), wrapper);
        List<AiProjectListVO> records = page.getRecords().stream().map(e -> {
            AiProjectListVO vo = new AiProjectListVO();
            vo.setId(e.getId());
            vo.setPurpose(e.getPurpose());
            vo.setContentType(e.getContentType());
            vo.setTopic(e.getTopic());
            vo.setSelectedTitle(e.getSelectedTitle());
            vo.setStatus(e.getStatus());
            vo.setUpdatedAt(e.getUpdatedAt());
            return vo;
        }).toList();
        return new PageResult<>(current, size, page.getTotal(), records);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(requireOwned(id).getId());
    }

    @Override
    public AiCreationProject requireOwned(Long id) {
        AiCreationProject entity = mapper.selectById(id);
        if (entity == null || !requireAgentCode().equals(entity.getAgentCode())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "创作项目不存在");
        }
        return entity;
    }

    @Override
    public int updateById(AiCreationProject entity) {
        return mapper.updateById(entity);
    }

    @Override
    public AiProjectVO toVO(AiCreationProject e) {
        AiProjectVO vo = new AiProjectVO();
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
        vo.setMaterialRefs(parse(e.getMaterialRefs(), AiMaterialRefsVO.class));
        vo.setFactDigest(parse(e.getFactDigest(), AiFactDigestVO.class));
        vo.setStrategy(parse(e.getStrategy(), AiStrategyVO.class));
        vo.setTitles(parseList(e.getTitles(), AiTitleVO.class));
        vo.setOutline(parse(e.getOutline(), AiOutlineVO.class));
        vo.setScores(parse(e.getScores(), AiScoresVO.class));
        vo.setImages(parseList(e.getImages(), AiImageVO.class));
        vo.setAuditLog(parseList(e.getAuditLog(), AiAuditItemVO.class));
        vo.setWarnings(parseList(e.getWarnings(), String.class));
        materialAssembler.resolveRefNames(vo);
        return vo;
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
