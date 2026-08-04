package com.dayan.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.service.entity.ServiceVisitRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * service_visit_record 数据访问层。
 */
@Mapper
public interface ServiceVisitRecordMapper extends BaseMapper<ServiceVisitRecord> {
}
