package com.dayan.butler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.butler.entity.ButlerServiceRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * butler_service_record 数据访问层。
 */
@Mapper
public interface ButlerServiceRecordMapper extends BaseMapper<ButlerServiceRecord> {
}
