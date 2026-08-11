package com.dayan.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.agent.dto.AgentCardCreateDTO;
import com.dayan.agent.dto.AgentCardQueryDTO;
import com.dayan.agent.dto.AgentCardUpdateDTO;
import com.dayan.agent.entity.AgentCard;
import com.dayan.agent.mapper.AgentCardMapper;
import com.dayan.agent.service.AgentCardService;
import com.dayan.agent.vo.AgentCardVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.mybatis.context.ContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 代理人电子名片服务实现。
 *
 * <p>所有查询/操作强制按 agentCode（当前登录代理人）过滤，确保代理人只能操作自己的名片。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCardServiceImpl implements AgentCardService {

    private final AgentCardMapper cardMapper;

    private static final DateTimeFormatter CODE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public PageResult<AgentCardVO> page(AgentCardQueryDTO query) {
        String agentCode = requireCurrentAgentCode();

        LambdaQueryWrapper<AgentCard> wrapper = new LambdaQueryWrapper<AgentCard>()
                .eq(AgentCard::getAgentCode, agentCode)
                .orderByAsc(AgentCard::getSortOrder)
                .orderByDesc(AgentCard::getCreatedAt);

        // keyword: 模糊搜索名片名称/显示姓名/手机号
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(AgentCard::getCardName, kw)
                    .or().like(AgentCard::getDisplayName, kw)
                    .or().like(AgentCard::getPhone, kw));
        }
        if (query.getStatus() != null) {
            wrapper.eq(AgentCard::getStatus, query.getStatus());
        }

        Page<AgentCard> page = cardMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<AgentCardVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AgentCardCreateDTO dto) {
        String agentCode = requireCurrentAgentCode();
        String channelCode = ContextHolder.getChannelCode();

        AgentCard card = new AgentCard();
        card.setAgentCode(agentCode);
        card.setChannelCode(channelCode != null ? channelCode : "");
        card.setCardCode(generateCardCode(agentCode));
        card.setCardName(dto.getCardName());
        card.setDisplayName(dto.getDisplayName());
        card.setTitle(dto.getTitle());
        card.setPhone(dto.getPhone());
        card.setWechat(dto.getWechat());
        card.setEmail(dto.getEmail());
        card.setCompany(dto.getCompany());
        card.setAddress(dto.getAddress());
        card.setAvatar(dto.getAvatar());
        card.setIntro(dto.getIntro());
        card.setTags(dto.getTags());
        card.setSortOrder(0);
        card.setStatus(1);

        cardMapper.insert(card);
        log.info("[Card] 新增名片: cardCode={}, agentCode={}", card.getCardCode(), agentCode);
        return card.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AgentCardUpdateDTO dto) {
        AgentCard card = requireOwnership(id);

        if (dto.getCardName() != null) card.setCardName(dto.getCardName());
        if (dto.getDisplayName() != null) card.setDisplayName(dto.getDisplayName());
        if (dto.getTitle() != null) card.setTitle(dto.getTitle());
        if (dto.getPhone() != null) card.setPhone(dto.getPhone());
        if (dto.getWechat() != null) card.setWechat(dto.getWechat());
        if (dto.getEmail() != null) card.setEmail(dto.getEmail());
        if (dto.getCompany() != null) card.setCompany(dto.getCompany());
        if (dto.getAddress() != null) card.setAddress(dto.getAddress());
        if (dto.getAvatar() != null) card.setAvatar(dto.getAvatar());
        if (dto.getIntro() != null) card.setIntro(dto.getIntro());
        if (dto.getTags() != null) card.setTags(dto.getTags());
        if (dto.getSortOrder() != null) card.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) card.setStatus(dto.getStatus());

        cardMapper.updateById(card);
        log.info("[Card] 更新名片: id={}, cardCode={}", id, card.getCardCode());
    }

    @Override
    public AgentCardVO detail(Long id) {
        AgentCard card = requireOwnership(id);
        return toVO(card);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AgentCard card = requireOwnership(id);
        cardMapper.deleteById(id);
        log.info("[Card] 删除名片: id={}, cardCode={}", id, card.getCardCode());
    }

    // ===== 内部方法 =====

    /**
     * 从 Sa-Token 上下文获取当前代理人编码。
     */
    private String requireCurrentAgentCode() {
        String agentCode = ContextHolder.getAccountCode();
        if (!StringUtils.hasText(agentCode)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return agentCode;
    }

    /**
     * 加载名片并校验归属权（只能操作自己的名片）。
     */
    private AgentCard requireOwnership(Long id) {
        String agentCode = requireCurrentAgentCode();
        AgentCard card = cardMapper.selectById(id);
        if (card == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "名片不存在");
        }
        if (!agentCode.equals(card.getAgentCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此名片");
        }
        return card;
    }

    @Override
    public AgentCardVO getFirstByAgent(String agentCode) {
        AgentCard card = cardMapper.selectOne(new LambdaQueryWrapper<AgentCard>()
                .eq(AgentCard::getAgentCode, agentCode)
                .eq(AgentCard::getStatus, 1)
                .orderByAsc(AgentCard::getSortOrder)
                .last("LIMIT 1"));
        return card != null ? toVO(card) : null;
    }

    /**
     * 生成名片编码：AC + yyyyMMdd + 4位序号（代理人内自增）。
     */
    private String generateCardCode(String agentCode) {
        String dateStr = LocalDateTime.now().format(CODE_DATE_FMT);
        Long count = cardMapper.selectCount(new LambdaQueryWrapper<AgentCard>()
                .eq(AgentCard::getAgentCode, agentCode)
                .likeRight(AgentCard::getCardCode, "AC" + dateStr));
        long seq = (count == null ? 0 : count) + 1;
        return String.format("AC%s%04d", dateStr, seq);
    }

    private AgentCardVO toVO(AgentCard card) {
        AgentCardVO vo = new AgentCardVO();
        vo.setId(card.getId());
        vo.setCardCode(card.getCardCode());
        vo.setAgentCode(card.getAgentCode());
        vo.setChannelCode(card.getChannelCode());
        vo.setCardName(card.getCardName());
        vo.setDisplayName(card.getDisplayName());
        vo.setTitle(card.getTitle());
        vo.setPhone(card.getPhone());
        vo.setWechat(card.getWechat());
        vo.setEmail(card.getEmail());
        vo.setCompany(card.getCompany());
        vo.setAddress(card.getAddress());
        vo.setAvatar(card.getAvatar());
        vo.setIntro(card.getIntro());
        vo.setTags(card.getTags());
        vo.setSortOrder(card.getSortOrder());
        vo.setStatus(card.getStatus());
        vo.setCreatedAt(card.getCreatedAt());
        vo.setUpdatedAt(card.getUpdatedAt());
        return vo;
    }
}
