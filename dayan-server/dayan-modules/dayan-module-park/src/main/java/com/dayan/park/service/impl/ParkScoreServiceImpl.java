package com.dayan.park.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.park.dto.ParkScoreUpdateDTO;
import com.dayan.park.entity.ParkScore;
import com.dayan.park.mapper.ParkScoreMapper;
import com.dayan.park.service.ParkScoreService;
import com.dayan.park.vo.ParkScoreVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 机构评分服务实现（一对一 upsert）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkScoreServiceImpl implements ParkScoreService {

    private final ParkScoreMapper scoreMapper;

    @Override
    public ParkScoreVO getByParkCode(String parkCode) {
        ParkScore score = scoreMapper.selectOne(new LambdaQueryWrapper<ParkScore>()
                .eq(ParkScore::getParkCode, parkCode));
        return toVO(score);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upsert(String parkCode, ParkScoreUpdateDTO dto) {
        ParkScore existing = scoreMapper.selectOne(new LambdaQueryWrapper<ParkScore>()
                .eq(ParkScore::getParkCode, parkCode));

        if (existing == null) {
            ParkScore entity = new ParkScore();
            entity.setParkCode(parkCode);
            applyDto(entity, dto);
            scoreMapper.insert(entity);
            log.info("创建机构评分: parkCode={}", parkCode);
        } else {
            ParkScore update = new ParkScore();
            update.setId(existing.getId());
            applyDto(update, dto);
            scoreMapper.updateById(update);
            log.info("更新机构评分: parkCode={}", parkCode);
        }
    }

    private void applyDto(ParkScore entity, ParkScoreUpdateDTO dto) {
        if (dto.getScoreTotal() != null) entity.setScoreTotal(dto.getScoreTotal());
        if (dto.getScoreEnvironment() != null) entity.setScoreEnvironment(dto.getScoreEnvironment());
        if (dto.getScoreRecreation() != null) entity.setScoreRecreation(dto.getScoreRecreation());
        if (dto.getScoreNursing() != null) entity.setScoreNursing(dto.getScoreNursing());
        if (dto.getScoreFood() != null) entity.setScoreFood(dto.getScoreFood());
        if (dto.getScoreService() != null) entity.setScoreService(dto.getScoreService());
        if (dto.getScorePrice() != null) entity.setScorePrice(dto.getScorePrice());
        if (dto.getScoreDescription() != null) entity.setScoreDescription(dto.getScoreDescription());
    }

    private ParkScoreVO toVO(ParkScore entity) {
        ParkScoreVO vo = new ParkScoreVO();
        if (entity != null) {
            vo.setId(entity.getId());
            vo.setParkCode(entity.getParkCode());
            vo.setScoreTotal(entity.getScoreTotal());
            vo.setScoreEnvironment(entity.getScoreEnvironment());
            vo.setScoreRecreation(entity.getScoreRecreation());
            vo.setScoreNursing(entity.getScoreNursing());
            vo.setScoreFood(entity.getScoreFood());
            vo.setScoreService(entity.getScoreService());
            vo.setScorePrice(entity.getScorePrice());
            vo.setScoreDescription(entity.getScoreDescription());
            vo.setCreatedAt(entity.getCreatedAt());
            vo.setUpdatedAt(entity.getUpdatedAt());
        }
        return vo;
    }
}
