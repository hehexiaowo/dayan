package com.dayan.distributor.service;

import com.dayan.distributor.dto.DistributorInfoCreateDTO;
import com.dayan.distributor.dto.DistributorInfoQueryDTO;
import com.dayan.distributor.dto.DistributorInfoUpdateDTO;
import com.dayan.distributor.vo.DistributorInfoVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 分销商信息（distributor_info）服务。
 *
 * <p>{@code distributor_info} 为平台共享表（{@code DayanTenantHandler} 忽略此前缀），
 * 查询/写入不带 channel_code 隔离条件。
 */
public interface DistributorInfoService {

    /** 分页查询 */
    PageResult<DistributorInfoVO> page(DistributorInfoQueryDTO query);

    /** 全量列表查询 */
    List<DistributorInfoVO> list(DistributorInfoQueryDTO query);

    /** 详情（按 distributorCode） */
    DistributorInfoVO getDetail(String distributorCode);

    /** 新增，返回生成的 distributorCode */
    String create(DistributorInfoCreateDTO dto);

    /** 修改（按 distributorCode） */
    void update(String distributorCode, DistributorInfoUpdateDTO dto);

    /** 删除（按 distributorCode） */
    void delete(String distributorCode);
}
