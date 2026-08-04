package com.dayan.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.service.entity.ButlerInfoView;
import org.apache.ibatis.annotations.Mapper;

/**
 * butler_info 表只读 Mapper（跨模块轻量引用，仅用于分配管家时查全名/校验在职状态）。
 */
@Mapper
public interface ButlerInfoViewMapper extends BaseMapper<ButlerInfoView> {
}
