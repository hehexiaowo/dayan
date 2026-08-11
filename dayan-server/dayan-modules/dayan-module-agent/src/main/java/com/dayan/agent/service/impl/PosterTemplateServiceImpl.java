package com.dayan.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.agent.entity.PosterTemplate;
import com.dayan.agent.mapper.PosterTemplateMapper;
import com.dayan.agent.service.PosterTemplateService;
import com.dayan.agent.vo.PosterTemplateVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PosterTemplateServiceImpl implements PosterTemplateService {

    private final PosterTemplateMapper posterTemplateMapper;

    @Override
    public List<PosterTemplateVO> listActive(String categoryCode) {
        LambdaQueryWrapper<PosterTemplate> wrapper = new LambdaQueryWrapper<PosterTemplate>()
                .eq(PosterTemplate::getStatus, 1)
                .eq(categoryCode != null && !categoryCode.isEmpty(),
                        PosterTemplate::getCategoryCode, categoryCode)
                .orderByAsc(PosterTemplate::getSortOrder)
                .orderByDesc(PosterTemplate::getCreatedAt);
        return posterTemplateMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public PosterTemplateVO getDetail(String templateCode) {
        PosterTemplate template = posterTemplateMapper.selectOne(
                new LambdaQueryWrapper<PosterTemplate>()
                        .eq(PosterTemplate::getTemplateCode, templateCode)
                        .eq(PosterTemplate::getStatus, 1)
                        .last("LIMIT 1"));
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "海报模板不存在: " + templateCode);
        }
        return toVO(template);
    }

    private PosterTemplateVO toVO(PosterTemplate t) {
        PosterTemplateVO vo = new PosterTemplateVO();
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
