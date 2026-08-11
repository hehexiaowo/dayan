package com.dayan.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.agent.dto.AgentFavoriteAddDTO;
import com.dayan.agent.dto.AgentFavoriteQueryDTO;
import com.dayan.agent.entity.AgentFavorite;
import com.dayan.agent.mapper.AgentFavoriteMapper;
import com.dayan.agent.service.AgentFavoriteService;
import com.dayan.agent.vo.AgentFavoriteVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 代理人收藏（agent_favorite）服务实现。
 *
 * <p>同一 agent_code + target_type + target_code 唯一（uk_agent_target 兜底），重复收藏幂等返回。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentFavoriteServiceImpl implements AgentFavoriteService {

    private final AgentFavoriteMapper favoriteMapper;

    @Override
    public PageResult<AgentFavoriteVO> page(AgentFavoriteQueryDTO query) {
        LambdaQueryWrapper<AgentFavorite> wrapper = new LambdaQueryWrapper<AgentFavorite>()
                .orderByDesc(AgentFavorite::getCreatedAt);
        if (query.getAgentCode() != null && !query.getAgentCode().isEmpty()) {
            wrapper.eq(AgentFavorite::getAgentCode, query.getAgentCode());
        }
        if (query.getTargetType() != null) {
            wrapper.eq(AgentFavorite::getTargetType, query.getTargetType());
        }
        if (query.getTargetCode() != null && !query.getTargetCode().isEmpty()) {
            wrapper.eq(AgentFavorite::getTargetCode, query.getTargetCode());
        }
        Page<AgentFavorite> page = favoriteMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<AgentFavoriteVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(AgentFavoriteAddDTO dto) {
        // 幂等：已存在则返回既有 id
        AgentFavorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<AgentFavorite>()
                .eq(AgentFavorite::getAgentCode, dto.getAgentCode())
                .eq(AgentFavorite::getTargetType, dto.getTargetType())
                .eq(AgentFavorite::getTargetCode, dto.getTargetCode())
                .last("LIMIT 1"));
        if (existing != null) {
            return existing.getId();
        }
        AgentFavorite entity = new AgentFavorite();
        entity.setAgentCode(dto.getAgentCode());
        entity.setTargetType(dto.getTargetType());
        entity.setTargetCode(dto.getTargetCode());
        favoriteMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        AgentFavorite existing = favoriteMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收藏记录不存在: id=" + id);
        }
        favoriteMapper.deleteById(id);
    }

    @Override
    public List<AgentFavoriteVO> listByAgent(String agentCode) {
        List<AgentFavorite> list = favoriteMapper.selectList(new LambdaQueryWrapper<AgentFavorite>()
                .eq(AgentFavorite::getAgentCode, agentCode)
                .orderByDesc(AgentFavorite::getCreatedAt));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addForAgent(String agentCode, Integer targetType, String targetCode) {
        // 幂等：已存在则返回既有 id
        AgentFavorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<AgentFavorite>()
                .eq(AgentFavorite::getAgentCode, agentCode)
                .eq(AgentFavorite::getTargetType, targetType)
                .eq(AgentFavorite::getTargetCode, targetCode)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing.getId();
        }
        AgentFavorite entity = new AgentFavorite();
        entity.setAgentCode(agentCode);
        entity.setTargetType(targetType);
        entity.setTargetCode(targetCode);
        favoriteMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void removeByTarget(String agentCode, Integer targetType, String targetCode) {
        // 带 agentCode 条件做归属校验，幂等（不存在不报错）
        favoriteMapper.delete(new LambdaQueryWrapper<AgentFavorite>()
                .eq(AgentFavorite::getAgentCode, agentCode)
                .eq(AgentFavorite::getTargetType, targetType)
                .eq(AgentFavorite::getTargetCode, targetCode));
    }

    @Override
    public List<String> listTargetCodes(String agentCode, Integer targetType) {
        return favoriteMapper.selectList(new LambdaQueryWrapper<AgentFavorite>()
                        .select(AgentFavorite::getTargetCode)
                        .eq(AgentFavorite::getAgentCode, agentCode)
                        .eq(AgentFavorite::getTargetType, targetType))
                .stream()
                .map(AgentFavorite::getTargetCode)
                .toList();
    }

    private AgentFavoriteVO toVO(AgentFavorite entity) {
        AgentFavoriteVO vo = new AgentFavoriteVO();
        vo.setId(entity.getId());
        vo.setAgentCode(entity.getAgentCode());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetCode(entity.getTargetCode());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
