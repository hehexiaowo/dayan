package com.dayan.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.client.dto.ClientFamilyMemberCreateDTO;
import com.dayan.client.dto.ClientFamilyMemberUpdateDTO;
import com.dayan.client.entity.ClientFamilyMember;
import com.dayan.client.mapper.ClientFamilyMemberMapper;
import com.dayan.client.service.ClientFamilyMemberService;
import com.dayan.client.vo.ClientFamilyMemberVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 客户家庭成员服务实现。
 *
 * <p>同客户同关系同姓名唯一校验（uk_client_member：client_code + relation + member_name）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientFamilyMemberServiceImpl implements ClientFamilyMemberService {

    private final ClientFamilyMemberMapper familyMemberMapper;

    @Override
    public List<ClientFamilyMemberVO> listByClient(String clientCode) {
        List<ClientFamilyMember> list = familyMemberMapper.selectList(
                new LambdaQueryWrapper<ClientFamilyMember>()
                        .eq(ClientFamilyMember::getClientCode, clientCode)
                        .orderByAsc(ClientFamilyMember::getSortOrder)
                        .orderByDesc(ClientFamilyMember::getCreatedAt));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ClientFamilyMemberCreateDTO dto) {
        // 唯一性校验：client_code + relation + member_name
        Long count = familyMemberMapper.selectCount(new LambdaQueryWrapper<ClientFamilyMember>()
                .eq(ClientFamilyMember::getClientCode, dto.getClientCode())
                .eq(ClientFamilyMember::getRelation, dto.getRelation())
                .eq(ClientFamilyMember::getMemberName, dto.getMemberName()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "同客户下已存在该关系与姓名的家庭成员");
        }

        ClientFamilyMember entity = new ClientFamilyMember();
        entity.setClientCode(dto.getClientCode());
        entity.setMemberName(dto.getMemberName());
        entity.setRelation(dto.getRelation());
        entity.setGender(dto.getGender());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setIsEmergencyContact(dto.getIsEmergencyContact() == null ? 0 : dto.getIsEmergencyContact());
        entity.setIsPrimaryContact(dto.getIsPrimaryContact() == null ? 0 : dto.getIsPrimaryContact());
        entity.setIsDecisionMaker(dto.getIsDecisionMaker() == null ? 0 : dto.getIsDecisionMaker());
        entity.setAddress(dto.getAddress());
        entity.setRemark(dto.getRemark());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        familyMemberMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ClientFamilyMemberUpdateDTO dto) {
        ClientFamilyMember existing = selectById(id);
        // 若 relation/memberName 变更，需重新做唯一性校验
        String newRelation = dto.getRelation() != null ? dto.getRelation() : existing.getRelation();
        String newMemberName = dto.getMemberName() != null ? dto.getMemberName() : existing.getMemberName();
        if (dto.getRelation() != null || dto.getMemberName() != null) {
            Long count = familyMemberMapper.selectCount(new LambdaQueryWrapper<ClientFamilyMember>()
                    .eq(ClientFamilyMember::getClientCode, existing.getClientCode())
                    .eq(ClientFamilyMember::getRelation, newRelation)
                    .eq(ClientFamilyMember::getMemberName, newMemberName)
                    .ne(ClientFamilyMember::getId, id));
            if (count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS,
                        "同客户下已存在该关系与姓名的家庭成员");
            }
        }

        ClientFamilyMember update = new ClientFamilyMember();
        update.setId(existing.getId());
        if (dto.getMemberName() != null) update.setMemberName(dto.getMemberName());
        if (dto.getRelation() != null) update.setRelation(dto.getRelation());
        if (dto.getGender() != null) update.setGender(dto.getGender());
        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getEmail() != null) update.setEmail(dto.getEmail());
        if (dto.getIsEmergencyContact() != null) update.setIsEmergencyContact(dto.getIsEmergencyContact());
        if (dto.getIsPrimaryContact() != null) update.setIsPrimaryContact(dto.getIsPrimaryContact());
        if (dto.getIsDecisionMaker() != null) update.setIsDecisionMaker(dto.getIsDecisionMaker());
        if (dto.getAddress() != null) update.setAddress(dto.getAddress());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        familyMemberMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        selectById(id);
        familyMemberMapper.deleteById(id);
    }

    private ClientFamilyMember selectById(Long id) {
        ClientFamilyMember entity = familyMemberMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "家庭成员不存在: " + id);
        }
        return entity;
    }

    private ClientFamilyMemberVO toVO(ClientFamilyMember entity) {
        ClientFamilyMemberVO vo = new ClientFamilyMemberVO();
        vo.setId(entity.getId());
        vo.setClientCode(entity.getClientCode());
        vo.setMemberName(entity.getMemberName());
        vo.setRelation(entity.getRelation());
        vo.setGender(entity.getGender());
        vo.setPhone(entity.getPhone());
        vo.setEmail(entity.getEmail());
        vo.setIsEmergencyContact(entity.getIsEmergencyContact());
        vo.setIsPrimaryContact(entity.getIsPrimaryContact());
        vo.setIsDecisionMaker(entity.getIsDecisionMaker());
        vo.setAddress(entity.getAddress());
        vo.setRemark(entity.getRemark());
        vo.setStatus(entity.getStatus());
        vo.setSortOrder(entity.getSortOrder());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
