package com.dayan.lead.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.lead.entity.LeadInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * lead_info 数据访问层。
 */
@Mapper
public interface LeadInfoMapper extends BaseMapper<LeadInfo> {
}
