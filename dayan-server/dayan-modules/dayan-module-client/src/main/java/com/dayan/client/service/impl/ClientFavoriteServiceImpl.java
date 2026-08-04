package com.dayan.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.client.dto.ClientFavoriteAddDTO;
import com.dayan.client.dto.ClientFavoriteQueryDTO;
import com.dayan.client.entity.ClientFavorite;
import com.dayan.client.mapper.ClientFavoriteMapper;
import com.dayan.client.service.ClientFavoriteService;
import com.dayan.client.vo.ClientFavoriteVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 客户收藏服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientFavoriteServiceImpl implements ClientFavoriteService {

    private final ClientFavoriteMapper favoriteMapper;

    @Override
    public PageResult<ClientFavoriteVO> page(ClientFavoriteQueryDTO query) {
        LambdaQueryWrapper<ClientFavorite> wrapper = new LambdaQueryWrapper<ClientFavorite>()
                .orderByDesc(ClientFavorite::getCreatedAt);
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(ClientFavorite::getClientCode, query.getClientCode());
        }
        if (query.getTargetType() != null) {
            wrapper.eq(ClientFavorite::getTargetType, query.getTargetType());
        }
        if (query.getTargetCode() != null && !query.getTargetCode().isEmpty()) {
            wrapper.eq(ClientFavorite::getTargetCode, query.getTargetCode());
        }
        Page<ClientFavorite> page = favoriteMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ClientFavoriteVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(ClientFavoriteAddDTO dto) {
        // 同客户同对象去重校验
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<ClientFavorite>()
                .eq(ClientFavorite::getClientCode, dto.getClientCode())
                .eq(ClientFavorite::getTargetType, dto.getTargetType())
                .eq(ClientFavorite::getTargetCode, dto.getTargetCode()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "已收藏该对象");
        }

        ClientFavorite entity = new ClientFavorite();
        entity.setClientCode(dto.getClientCode());
        entity.setTargetType(dto.getTargetType());
        entity.setTargetCode(dto.getTargetCode());
        entity.setTargetName(dto.getTargetName());
        entity.setRemark(dto.getRemark());
        favoriteMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        ClientFavorite existing = favoriteMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收藏记录不存在: " + id);
        }
        favoriteMapper.deleteById(id);
    }

    @Override
    public List<ClientFavoriteVO> listByClient(String clientCode) {
        List<ClientFavorite> list = favoriteMapper.selectList(new LambdaQueryWrapper<ClientFavorite>()
                .eq(ClientFavorite::getClientCode, clientCode)
                .orderByDesc(ClientFavorite::getCreatedAt));
        return list.stream().map(this::toVO).toList();
    }

    private ClientFavoriteVO toVO(ClientFavorite entity) {
        ClientFavoriteVO vo = new ClientFavoriteVO();
        vo.setId(entity.getId());
        vo.setClientCode(entity.getClientCode());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetCode(entity.getTargetCode());
        vo.setTargetName(entity.getTargetName());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
