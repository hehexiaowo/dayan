package com.dayan.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.system.entity.SystemDictBusiness;
import com.dayan.system.mapper.SystemDictBusinessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 业务字典服务（system_dict_business）。
 *
 * <p>按 domain（业务域）+ dictType 组织，区别于通用字典 system_dict_common。
 * 供各业务域维护自身专属字典项。
 */
@Service
@RequiredArgsConstructor
public class SystemDictBusinessService {

    private final SystemDictBusinessMapper businessDictMapper;

    /**
     * 分页查询（按 dictType / domain 过滤）。
     */
    public PageResult<SystemDictBusiness> page(long current, long size, String dictType, String domain) {
        LambdaQueryWrapper<SystemDictBusiness> wrapper = new LambdaQueryWrapper<SystemDictBusiness>()
                .eq(dictType != null && !dictType.isEmpty(), SystemDictBusiness::getDictType, dictType)
                .eq(domain != null && !domain.isEmpty(), SystemDictBusiness::getDomain, domain)
                .orderByAsc(SystemDictBusiness::getDomain)
                .orderByAsc(SystemDictBusiness::getSortOrder);
        Page<SystemDictBusiness> page = businessDictMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(current, size, page.getTotal(), page.getRecords());
    }

    /**
     * 按 domain 全量列表（下拉用）。
     */
    public List<SystemDictBusiness> listByDomain(String domain) {
        return businessDictMapper.selectList(new LambdaQueryWrapper<SystemDictBusiness>()
                .eq(domain != null && !domain.isEmpty(), SystemDictBusiness::getDomain, domain)
                .eq(SystemDictBusiness::getStatus, 1)
                .orderByAsc(SystemDictBusiness::getSortOrder));
    }

    /**
     * 按 dictType 全量列表（供业务域以字典为数据源的场景，如内容分类）。
     *
     * @param onlyEnabled true 时仅返回启用项
     */
    public List<SystemDictBusiness> listByType(String dictType, boolean onlyEnabled) {
        return businessDictMapper.selectList(new LambdaQueryWrapper<SystemDictBusiness>()
                .eq(SystemDictBusiness::getDictType, dictType)
                .eq(onlyEnabled, SystemDictBusiness::getStatus, 1)
                .orderByAsc(SystemDictBusiness::getSortOrder)
                .orderByAsc(SystemDictBusiness::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemDictBusiness dict) {
        // (dictType, dictCode) 唯一校验
        Long count = businessDictMapper.selectCount(new LambdaQueryWrapper<SystemDictBusiness>()
                .eq(SystemDictBusiness::getDictType, dict.getDictType())
                .eq(SystemDictBusiness::getDictCode, dict.getDictCode()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "字典编码已存在: " + dict.getDictCode());
        }
        if (dict.getStatus() == null) dict.setStatus(1);
        if (dict.getSortOrder() == null) dict.setSortOrder(0);
        businessDictMapper.insert(dict);
        return dict.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SystemDictBusiness dict) {
        SystemDictBusiness existing = businessDictMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典项不存在: id=" + id);
        }
        dict.setId(id);
        businessDictMapper.updateById(dict);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SystemDictBusiness existing = businessDictMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典项不存在: id=" + id);
        }
        businessDictMapper.deleteById(id);
    }
}
