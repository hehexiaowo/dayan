package com.dayan.tool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.tool.entity.ToolPosterTemplate;
import com.dayan.tool.mapper.ToolPosterTemplateMapper;
import com.dayan.tool.service.ToolPosterTemplateService;
import com.dayan.tool.vo.ToolPosterTemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolPosterTemplateServiceImpl implements ToolPosterTemplateService {

    private final ToolPosterTemplateMapper posterTemplateMapper;

    @Override
    public List<ToolPosterTemplateVO> listActive(String categoryCode) {
        LambdaQueryWrapper<ToolPosterTemplate> wrapper = new LambdaQueryWrapper<ToolPosterTemplate>()
                .eq(ToolPosterTemplate::getStatus, 1)
                .eq(categoryCode != null && !categoryCode.isEmpty(),
                        ToolPosterTemplate::getCategoryCode, categoryCode)
                .orderByAsc(ToolPosterTemplate::getSortOrder)
                .orderByDesc(ToolPosterTemplate::getCreatedAt);
        return posterTemplateMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public ToolPosterTemplateVO getDetail(String templateCode) {
        ToolPosterTemplate template = posterTemplateMapper.selectOne(
                new LambdaQueryWrapper<ToolPosterTemplate>()
                        .eq(ToolPosterTemplate::getTemplateCode, templateCode)
                        .eq(ToolPosterTemplate::getStatus, 1)
                        .last("LIMIT 1"));
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "海报模板不存在: " + templateCode);
        }
        return toVO(template);
    }

    private ToolPosterTemplateVO toVO(ToolPosterTemplate t) {
        ToolPosterTemplateVO vo = new ToolPosterTemplateVO();
        vo.setId(t.getId());
        vo.setTemplateCode(t.getTemplateCode());
        vo.setTitle(t.getTitle());
        vo.setSubtitle(t.getSubtitle());
        vo.setBodyText(t.getBodyText());
        vo.setCoverImage(t.getCoverImage());
        vo.setCategoryCode(t.getCategoryCode());
        vo.setCategoryName(t.getCategoryName());
        vo.setSortOrder(t.getSortOrder());
        vo.setCreatedAt(t.getCreatedAt());
        return vo;
    }
}
