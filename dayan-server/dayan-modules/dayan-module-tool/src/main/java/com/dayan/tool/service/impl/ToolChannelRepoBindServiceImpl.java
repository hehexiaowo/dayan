package com.dayan.tool.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.system.service.SystemKnowledgeRepoService;
import com.dayan.system.vo.SystemKnowledgeRepoVO;
import com.dayan.tool.entity.ToolChannelRepoBind;
import com.dayan.tool.mapper.ToolChannelRepoBindMapper;
import com.dayan.tool.service.ToolChannelRepoBindService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 渠道问答人物补充知识库绑定服务实现 */
@Service
@RequiredArgsConstructor
public class ToolChannelRepoBindServiceImpl implements ToolChannelRepoBindService {

    private final ToolChannelRepoBindMapper bindMapper;
    private final SystemKnowledgeRepoService knowledgeRepoService;

    @Override
    public List<Long> listRepoIds(String toolCode, String channelCode) {
        if (StrUtil.isBlank(toolCode) || StrUtil.isBlank(channelCode)) {
            return List.of();
        }
        return bindMapper.selectList(new LambdaQueryWrapper<ToolChannelRepoBind>()
                        .eq(ToolChannelRepoBind::getToolCode, toolCode)
                        .eq(ToolChannelRepoBind::getChannelCode, channelCode)
                        .orderByAsc(ToolChannelRepoBind::getId))
                .stream().map(ToolChannelRepoBind::getRepoId).toList();
    }

    @Override
    public List<Long> mergeRepoIds(String toolCode, List<Long> globalRepoIds) {
        Set<Long> merged = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(globalRepoIds)) {
            merged.addAll(globalRepoIds);
        }
        String channelCode = ContextHolder.getChannelCode();
        if (StrUtil.isNotBlank(channelCode)) {
            merged.addAll(listRepoIds(toolCode, channelCode));
        }
        return List.copyOf(merged);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveChannelRepos(String toolCode, String channelCode, List<Long> repoIds) {
        if (StrUtil.isBlank(toolCode) || StrUtil.isBlank(channelCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "人物编码与渠道编码不能为空");
        }
        List<Long> targets = repoIds == null ? List.of() : repoIds.stream().distinct().toList();
        // 归属校验：仅允许本渠道 + 后代渠道名下的渠道库
        Set<Long> allowed = knowledgeRepoService.listChannelScopeRepos(channelCode).stream()
                .map(SystemKnowledgeRepoVO::getId).collect(Collectors.toSet());
        for (Long repoId : targets) {
            if (repoId == null || !allowed.contains(repoId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "知识库不在可补充范围内: " + repoId);
            }
        }
        bindMapper.delete(new LambdaQueryWrapper<ToolChannelRepoBind>()
                .eq(ToolChannelRepoBind::getToolCode, toolCode)
                .eq(ToolChannelRepoBind::getChannelCode, channelCode));
        for (Long repoId : targets) {
            ToolChannelRepoBind bind = new ToolChannelRepoBind();
            bind.setToolCode(toolCode);
            bind.setChannelCode(channelCode);
            bind.setRepoId(repoId);
            bindMapper.insert(bind);
        }
    }
}
