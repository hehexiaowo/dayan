package com.dayan.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.service.entity.ServiceChangeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * service_change_record 数据访问层。
 */
@Mapper
public interface ServiceChangeRecordMapper extends BaseMapper<ServiceChangeRecord> {
}
