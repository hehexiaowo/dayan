package com.dayan.scene.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.scene.entity.ParkInfoView;
import org.apache.ibatis.annotations.Mapper;

/**
 * park_info 表只读 Mapper（跨模块轻量引用，仅用于按 parkCode 批量取机构名称回填 parkName）。
 */
@Mapper
public interface ParkInfoViewMapper extends BaseMapper<ParkInfoView> {
}
