package com.dayan.butler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.butler.entity.ButlerInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * butler_info 数据访问层。
 */
@Mapper
public interface ButlerInfoMapper extends BaseMapper<ButlerInfo> {
}
