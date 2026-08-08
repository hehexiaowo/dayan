package com.dayan.equity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.equity.dto.EquityActivateQueryDTO;
import com.dayan.equity.entity.EquityActivate;
import com.dayan.equity.mapper.EquityActivateMapper;
import com.dayan.equity.service.EquityActivateService;
import com.dayan.equity.vo.EquityActivateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权益激活记录服务实现（仅查询）。
 */
@Service
@RequiredArgsConstructor
public class EquityActivateServiceImpl implements EquityActivateService {

    private final EquityActivateMapper activateMapper;

    @Override
    public PageResult<EquityActivateVO> page(EquityActivateQueryDTO query) {
        LambdaQueryWrapper<EquityActivate> wrapper = buildQueryWrapper(query);
        Page<EquityActivate> page = activateMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<EquityActivateVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<EquityActivateVO> list(EquityActivateQueryDTO query) {
        return activateMapper.selectList(buildQueryWrapper(query)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public EquityActivateVO getByEquityCode(String equityCode) {
        EquityActivate activate = activateMapper.selectOne(new LambdaQueryWrapper<EquityActivate>()
                .eq(EquityActivate::getEquityCode, equityCode)
                .orderByDesc(EquityActivate::getActivateTime)
                .last("LIMIT 1"));
        if (activate == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "权益无激活记录: " + equityCode);
        }
        return toVO(activate);
    }

    private LambdaQueryWrapper<EquityActivate> buildQueryWrapper(EquityActivateQueryDTO query) {
        LambdaQueryWrapper<EquityActivate> wrapper = new LambdaQueryWrapper<EquityActivate>()
                .orderByDesc(EquityActivate::getActivateTime);
        if (query.getActivateCode() != null && !query.getActivateCode().isEmpty()) {
            wrapper.eq(EquityActivate::getActivateCode, query.getActivateCode());
        }
        if (query.getEquityCode() != null && !query.getEquityCode().isEmpty()) {
            wrapper.eq(EquityActivate::getEquityCode, query.getEquityCode());
        }
        if (query.getTemplateCode() != null && !query.getTemplateCode().isEmpty()) {
            wrapper.eq(EquityActivate::getTemplateCode, query.getTemplateCode());
        }
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(EquityActivate::getClientCode, query.getClientCode());
        }
        if (query.getActivateChannel() != null) {
            wrapper.eq(EquityActivate::getActivateChannel, query.getActivateChannel());
        }
        if (query.getEquityCodes() != null && !query.getEquityCodes().isEmpty()) {
            wrapper.in(EquityActivate::getEquityCode, query.getEquityCodes());
        }
        return wrapper;
    }

    private EquityActivateVO toVO(EquityActivate entity) {
        EquityActivateVO vo = new EquityActivateVO();
        vo.setId(entity.getId());
        vo.setActivateCode(entity.getActivateCode());
        vo.setEquityCode(entity.getEquityCode());
        vo.setTemplateCode(entity.getTemplateCode());
        vo.setClientCode(entity.getClientCode());
        vo.setClientFullName(entity.getClientFullName());
        vo.setClientPhone(entity.getClientPhone());
        vo.setActivateChannel(entity.getActivateChannel());
        vo.setActivateSourceCode(entity.getActivateSourceCode());
        vo.setActivateTime(entity.getActivateTime());
        vo.setExpireTime(entity.getExpireTime());
        vo.setIsIdCardVerified(entity.getIsIdCardVerified());
        vo.setIsAgreementSigned(entity.getIsAgreementSigned());
        vo.setIpAddress(entity.getIpAddress());
        vo.setDeviceInfo(entity.getDeviceInfo());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
