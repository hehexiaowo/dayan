package com.dayan.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.client.dto.ClientCareNeedCreateDTO;
import com.dayan.client.dto.ClientCareNeedQueryDTO;
import com.dayan.client.dto.ClientCareNeedUpdateDTO;
import com.dayan.client.entity.ClientCareNeed;
import com.dayan.client.mapper.ClientCareNeedMapper;
import com.dayan.client.service.ClientCareNeedService;
import com.dayan.client.vo.ClientCareNeedVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 客户照护需求评估服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientCareNeedServiceImpl implements ClientCareNeedService {

    private final ClientCareNeedMapper careNeedMapper;

    @Override
    public PageResult<ClientCareNeedVO> page(ClientCareNeedQueryDTO query) {
        LambdaQueryWrapper<ClientCareNeed> wrapper = new LambdaQueryWrapper<ClientCareNeed>()
                .orderByDesc(ClientCareNeed::getCreatedAt);
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(ClientCareNeed::getClientCode, query.getClientCode());
        }
        if (query.getButlerCode() != null && !query.getButlerCode().isEmpty()) {
            wrapper.eq(ClientCareNeed::getButlerCode, query.getButlerCode());
        }
        if (query.getCareLevel() != null) {
            wrapper.eq(ClientCareNeed::getCareLevel, query.getCareLevel());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ClientCareNeed::getStatus, query.getStatus());
        }
        Page<ClientCareNeed> page = careNeedMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ClientCareNeedVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ClientCareNeedVO> listByClient(String clientCode) {
        List<ClientCareNeed> list = careNeedMapper.selectList(new LambdaQueryWrapper<ClientCareNeed>()
                .eq(ClientCareNeed::getClientCode, clientCode)
                .orderByDesc(ClientCareNeed::getCreatedAt));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ClientCareNeedCreateDTO dto) {
        ClientCareNeed entity = new ClientCareNeed();
        entity.setClientCode(dto.getClientCode());
        entity.setButlerCode(dto.getButlerCode());
        entity.setButlerFullName(dto.getButlerFullName());
        entity.setEvalDate(dto.getEvalDate());
        entity.setCareLevel(dto.getCareLevel());
        entity.setCareTypePreference(dto.getCareTypePreference());
        entity.setLivingPreference(dto.getLivingPreference());
        entity.setFoodPreference(dto.getFoodPreference());
        entity.setBudgetMin(dto.getBudgetMin());
        entity.setBudgetMax(dto.getBudgetMax());
        entity.setAreaPreference(dto.getAreaPreference());
        entity.setSpecialRequirements(dto.getSpecialRequirements());
        entity.setExpectedCheckinDate(dto.getExpectedCheckinDate());
        entity.setParkRecommendations(dto.getParkRecommendations());
        entity.setEvalResult(dto.getEvalResult());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setRemark(dto.getRemark());
        careNeedMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ClientCareNeedUpdateDTO dto) {
        ClientCareNeed existing = selectById(id);
        ClientCareNeed update = new ClientCareNeed();
        update.setId(existing.getId());
        if (dto.getButlerCode() != null) update.setButlerCode(dto.getButlerCode());
        if (dto.getButlerFullName() != null) update.setButlerFullName(dto.getButlerFullName());
        if (dto.getEvalDate() != null) update.setEvalDate(dto.getEvalDate());
        if (dto.getCareLevel() != null) update.setCareLevel(dto.getCareLevel());
        if (dto.getCareTypePreference() != null) update.setCareTypePreference(dto.getCareTypePreference());
        if (dto.getLivingPreference() != null) update.setLivingPreference(dto.getLivingPreference());
        if (dto.getFoodPreference() != null) update.setFoodPreference(dto.getFoodPreference());
        if (dto.getBudgetMin() != null) update.setBudgetMin(dto.getBudgetMin());
        if (dto.getBudgetMax() != null) update.setBudgetMax(dto.getBudgetMax());
        if (dto.getAreaPreference() != null) update.setAreaPreference(dto.getAreaPreference());
        if (dto.getSpecialRequirements() != null) update.setSpecialRequirements(dto.getSpecialRequirements());
        if (dto.getExpectedCheckinDate() != null) update.setExpectedCheckinDate(dto.getExpectedCheckinDate());
        if (dto.getParkRecommendations() != null) update.setParkRecommendations(dto.getParkRecommendations());
        if (dto.getEvalResult() != null) update.setEvalResult(dto.getEvalResult());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        careNeedMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        selectById(id);
        careNeedMapper.deleteById(id);
    }

    private ClientCareNeed selectById(Long id) {
        ClientCareNeed entity = careNeedMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "照护需求评估不存在: " + id);
        }
        return entity;
    }

    private ClientCareNeedVO toVO(ClientCareNeed entity) {
        ClientCareNeedVO vo = new ClientCareNeedVO();
        vo.setId(entity.getId());
        vo.setClientCode(entity.getClientCode());
        vo.setButlerCode(entity.getButlerCode());
        vo.setButlerFullName(entity.getButlerFullName());
        vo.setEvalDate(entity.getEvalDate());
        vo.setCareLevel(entity.getCareLevel());
        vo.setCareTypePreference(entity.getCareTypePreference());
        vo.setLivingPreference(entity.getLivingPreference());
        vo.setFoodPreference(entity.getFoodPreference());
        vo.setBudgetMin(entity.getBudgetMin());
        vo.setBudgetMax(entity.getBudgetMax());
        vo.setAreaPreference(entity.getAreaPreference());
        vo.setSpecialRequirements(entity.getSpecialRequirements());
        vo.setExpectedCheckinDate(entity.getExpectedCheckinDate());
        vo.setParkRecommendations(entity.getParkRecommendations());
        vo.setEvalResult(entity.getEvalResult());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
