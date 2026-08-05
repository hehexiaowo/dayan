package com.dayan.butler.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.butler.dto.ButlerInfoCreateDTO;
import com.dayan.butler.dto.ButlerInfoQueryDTO;
import com.dayan.butler.dto.ButlerInfoUpdateDTO;
import com.dayan.butler.entity.ButlerInfo;
import com.dayan.butler.mapper.ButlerInfoMapper;
import com.dayan.butler.service.ButlerInfoService;
import com.dayan.butler.vo.ButlerInfoVO;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管家信息服务实现。
 *
 * <p>管家编码生成：{@code "BT" + String.format("%05d", sequenceProvider.next("code:seq:BT:0"))}，
 * 全表唯一。新建默认 status=1（启用）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ButlerInfoServiceImpl implements ButlerInfoService {

    /** 管家编码前缀 */
    private static final String CODE_PREFIX = "BT";
    /** 序列键 */
    private static final String SEQ_KEY = "code:seq:BT:0";
    /** 默认状态：启用 */
    private static final int DEFAULT_STATUS = 1;

    private final ButlerInfoMapper butlerInfoMapper;
    private final SequenceProvider sequenceProvider;

    @Override
    public PageResult<ButlerInfoVO> page(ButlerInfoQueryDTO query) {
        LambdaQueryWrapper<ButlerInfo> wrapper = buildQueryWrapper(query);
        Page<ButlerInfo> page = butlerInfoMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ButlerInfoVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public List<ButlerInfoVO> list(ButlerInfoQueryDTO query) {
        LambdaQueryWrapper<ButlerInfo> wrapper = buildQueryWrapper(query);
        return butlerInfoMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public ButlerInfoVO getDetail(String butlerCode) {
        return toVO(requireButler(butlerCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ButlerInfoCreateDTO dto) {
        String butlerCode = generateButlerCode();

        ButlerInfo entity = new ButlerInfo();
        entity.setButlerCode(butlerCode);
        entity.setFullName(dto.getFullName());
        entity.setPhone(dto.getPhone());
        entity.setAvatar(dto.getAvatar());
        // organ_code 为 NOT NULL 列，未指定所属组织时兜底平台默认组织
        entity.setOrganCode(dto.getOrganCode() != null && !dto.getOrganCode().isEmpty()
                ? dto.getOrganCode() : "OR00000");
        entity.setButlerLevel(dto.getButlerLevel());
        entity.setStatus(dto.getStatus() == null ? DEFAULT_STATUS : dto.getStatus());
        entity.setRemark(dto.getRemark());

        butlerInfoMapper.insert(entity);
        log.info("创建管家成功: butlerCode={}, fullName={}", butlerCode, dto.getFullName());
        return butlerCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String butlerCode, ButlerInfoUpdateDTO dto) {
        ButlerInfo existing = requireButler(butlerCode);
        ButlerInfo update = new ButlerInfo();
        update.setId(existing.getId());

        if (dto.getFullName() != null) update.setFullName(dto.getFullName());
        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getAvatar() != null) update.setAvatar(dto.getAvatar());
        if (dto.getOrganCode() != null) update.setOrganCode(dto.getOrganCode());
        if (dto.getButlerLevel() != null) update.setButlerLevel(dto.getButlerLevel());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getRemark() != null) update.setRemark(dto.getRemark());

        butlerInfoMapper.updateById(update);
        log.info("更新管家成功: butlerCode={}", butlerCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String butlerCode) {
        ButlerInfo existing = requireButler(butlerCode);
        butlerInfoMapper.deleteById(existing.getId());
        log.info("删除管家成功: butlerCode={}", butlerCode);
    }

    // ====== 内部方法 ======

    private LambdaQueryWrapper<ButlerInfo> buildQueryWrapper(ButlerInfoQueryDTO query) {
        return new LambdaQueryWrapper<ButlerInfo>()
                .eq(query.getButlerCode() != null && !query.getButlerCode().isEmpty(),
                        ButlerInfo::getButlerCode, query.getButlerCode())
                .like(query.getFullName() != null && !query.getFullName().isEmpty(),
                        ButlerInfo::getFullName, query.getFullName())
                .eq(query.getPhone() != null && !query.getPhone().isEmpty(),
                        ButlerInfo::getPhone, query.getPhone())
                .eq(query.getOrganCode() != null && !query.getOrganCode().isEmpty(),
                        ButlerInfo::getOrganCode, query.getOrganCode())
                .eq(query.getButlerLevel() != null,
                        ButlerInfo::getButlerLevel, query.getButlerLevel())
                .eq(query.getStatus() != null, ButlerInfo::getStatus, query.getStatus())
                .orderByDesc(ButlerInfo::getCreatedAt);
    }

    ButlerInfo requireButler(String butlerCode) {
        ButlerInfo butler = butlerInfoMapper.selectOne(new LambdaQueryWrapper<ButlerInfo>()
                .eq(ButlerInfo::getButlerCode, butlerCode)
                .last("LIMIT 1"));
        if (butler == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "管家不存在: " + butlerCode);
        }
        return butler;
    }

    /** 生成管家编码：BT + 5 位序列 */
    private String generateButlerCode() {
        return CODE_PREFIX + String.format("%05d", sequenceProvider.next(SEQ_KEY));
    }

    private ButlerInfoVO toVO(ButlerInfo entity) {
        ButlerInfoVO vo = new ButlerInfoVO();
        vo.setId(entity.getId());
        vo.setButlerCode(entity.getButlerCode());
        vo.setFullName(entity.getFullName());
        vo.setPhone(entity.getPhone());
        vo.setAvatar(entity.getAvatar());
        vo.setOrganCode(entity.getOrganCode());
        vo.setButlerLevel(entity.getButlerLevel());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
