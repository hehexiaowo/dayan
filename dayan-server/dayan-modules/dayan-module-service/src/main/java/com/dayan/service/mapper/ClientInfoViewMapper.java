package com.dayan.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.service.entity.ClientInfoView;
import org.apache.ibatis.annotations.Mapper;

/**
 * client_info 表只读 Mapper（跨模块轻量引用，仅用于按 clientCode 取客户姓名回填 clientName）。
 */
@Mapper
public interface ClientInfoViewMapper extends BaseMapper<ClientInfoView> {
}
