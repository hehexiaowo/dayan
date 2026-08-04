package com.dayan.client.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.client.dto.ClientHealthProfileSaveDTO;
import com.dayan.client.entity.ClientHealthProfile;
import com.dayan.client.mapper.ClientHealthProfileMapper;
import com.dayan.client.service.ClientHealthProfileService;
import com.dayan.client.vo.ClientHealthProfileVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 客户健康档案服务实现（一客户一档案，upsert 模式）。
 *
 * <p>JSON 字段（慢性病/过敏/手术/用药等）作为 String 存储。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientHealthProfileServiceImpl implements ClientHealthProfileService {

    private final ClientHealthProfileMapper healthProfileMapper;

    @Override
    public ClientHealthProfileVO getByClient(String clientCode) {
        ClientHealthProfile profile = selectByClient(clientCode);
        return profile == null ? null : toVO(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdate(ClientHealthProfileSaveDTO dto) {
        ClientHealthProfile existing = selectByClient(dto.getClientCode());
        if (existing == null) {
            // insert
            ClientHealthProfile entity = toEntity(dto, null);
            entity.setLastAssessmentTime(LocalDateTime.now());
            healthProfileMapper.insert(entity);
            return entity.getId();
        }
        // update
        ClientHealthProfile update = toEntity(dto, existing.getId());
        update.setLastAssessmentTime(LocalDateTime.now());
        healthProfileMapper.updateById(update);
        return existing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String clientCode) {
        ClientHealthProfile existing = selectByClient(clientCode);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "客户健康档案不存在: " + clientCode);
        }
        healthProfileMapper.deleteById(existing.getId());
    }

    private ClientHealthProfile selectByClient(String clientCode) {
        return healthProfileMapper.selectOne(new LambdaQueryWrapper<ClientHealthProfile>()
                .eq(ClientHealthProfile::getClientCode, clientCode).last("LIMIT 1"));
    }

    private ClientHealthProfile toEntity(ClientHealthProfileSaveDTO dto, Long id) {
        ClientHealthProfile entity = new ClientHealthProfile();
        if (id != null) {
            entity.setId(id);
        }
        entity.setClientCode(dto.getClientCode());
        entity.setHeight(dto.getHeight());
        entity.setWeight(dto.getWeight());
        entity.setBloodType(dto.getBloodType());
        entity.setBloodPressure(dto.getBloodPressure());
        entity.setBloodSugar(dto.getBloodSugar());
        entity.setHeartRate(dto.getHeartRate());
        entity.setChronicDiseases(dto.getChronicDiseases());
        entity.setAllergyHistory(dto.getAllergyHistory());
        entity.setSurgeryHistory(dto.getSurgeryHistory());
        entity.setFamilyHistory(dto.getFamilyHistory());
        entity.setMedicationInfo(dto.getMedicationInfo());
        entity.setMobilityLevel(dto.getMobilityLevel());
        entity.setCognitiveLevel(dto.getCognitiveLevel());
        entity.setMentalStatus(dto.getMentalStatus());
        entity.setDietPreference(dto.getDietPreference());
        entity.setSleepQuality(dto.getSleepQuality());
        entity.setEmergencyContactName(dto.getEmergencyContactName());
        entity.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        entity.setEmergencyContactRelation(dto.getEmergencyContactRelation());
        entity.setHealthScore(dto.getHealthScore());
        entity.setRemark(dto.getRemark());
        return entity;
    }

    private ClientHealthProfileVO toVO(ClientHealthProfile entity) {
        ClientHealthProfileVO vo = new ClientHealthProfileVO();
        vo.setId(entity.getId());
        vo.setClientCode(entity.getClientCode());
        vo.setHeight(entity.getHeight());
        vo.setWeight(entity.getWeight());
        vo.setBloodType(entity.getBloodType());
        vo.setBloodPressure(entity.getBloodPressure());
        vo.setBloodSugar(entity.getBloodSugar());
        vo.setHeartRate(entity.getHeartRate());
        vo.setChronicDiseases(entity.getChronicDiseases());
        vo.setAllergyHistory(entity.getAllergyHistory());
        vo.setSurgeryHistory(entity.getSurgeryHistory());
        vo.setFamilyHistory(entity.getFamilyHistory());
        vo.setMedicationInfo(entity.getMedicationInfo());
        vo.setMobilityLevel(entity.getMobilityLevel());
        vo.setCognitiveLevel(entity.getCognitiveLevel());
        vo.setMentalStatus(entity.getMentalStatus());
        vo.setDietPreference(entity.getDietPreference());
        vo.setSleepQuality(entity.getSleepQuality());
        vo.setEmergencyContactName(entity.getEmergencyContactName());
        vo.setEmergencyContactPhone(entity.getEmergencyContactPhone());
        vo.setEmergencyContactRelation(entity.getEmergencyContactRelation());
        vo.setHealthScore(entity.getHealthScore());
        vo.setLastAssessmentTime(entity.getLastAssessmentTime());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
