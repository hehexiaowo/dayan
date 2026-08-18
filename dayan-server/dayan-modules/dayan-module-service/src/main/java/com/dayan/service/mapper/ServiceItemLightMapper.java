package com.dayan.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 服务项目轻量查询（服务会话「服务类型=服务项目名称」展示与筛选用）。
 *
 * <p>直读 service_item 表（goods 模块管理），service 模块仅只读，避免模块依赖。
 */
@Mapper
public interface ServiceItemLightMapper extends BaseMapper<ServiceItemLight> {

    /** 按项目编码查名称（VO 展示用；失败容错由调用方处理） */
    @Select("SELECT item_name FROM service_item WHERE item_code = #{itemCode} AND deleted = 0 LIMIT 1")
    String selectItemName(@Param("itemCode") String itemCode);
}
