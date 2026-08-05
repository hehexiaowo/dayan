package com.dayan.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.system.entity.SystemDictRegion;
import com.dayan.system.mapper.SystemDictRegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 行政区划服务。
 *
 * <p>查询 system_dict_region 表，供前端省市区三级联动下拉使用。
 * 数据量大（3300+ 条）但几乎永不变更，靠 DB 索引（idx_level / idx_parent_code）查询即可，不做缓存。
 */
@Service
@RequiredArgsConstructor
public class RegionService {

    private final SystemDictRegionMapper regionMapper;

    /**
     * 查省级行政区（level=1）。
     */
    public List<SystemDictRegion> listProvinces() {
        return regionMapper.selectList(new LambdaQueryWrapper<SystemDictRegion>()
                .eq(SystemDictRegion::getLevel, 1)
                .orderByAsc(SystemDictRegion::getSortOrder));
    }

    /**
     * 查某父级的下级行政区（市级 parentCode=省码 / 区级 parentCode=市码）。
     */
    public List<SystemDictRegion> listChildren(String parentCode) {
        return regionMapper.selectList(new LambdaQueryWrapper<SystemDictRegion>()
                .eq(SystemDictRegion::getParentCode, parentCode)
                .orderByAsc(SystemDictRegion::getSortOrder));
    }
}
