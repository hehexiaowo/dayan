package com.dayan.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.park.entity.SupplierInfoView;
import org.apache.ibatis.annotations.Mapper;

/**
 * supplier_info 表只读 Mapper（跨模块轻量引用，仅用于 park_info.supplierCode 校验）。
 */
@Mapper
public interface SupplierInfoViewMapper extends BaseMapper<SupplierInfoView> {
}
