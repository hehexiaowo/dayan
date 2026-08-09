package com.dayan.service.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.service.dto.ServiceEquityDemandCreateDTO;
import com.dayan.service.dto.ServiceEquityDemandQueryDTO;
import com.dayan.service.dto.ServiceEquityDemandUpdateDTO;
import com.dayan.service.vo.ServiceEquityDemandVO;

import java.util.List;

/**
 * 需求收集（service_equity_demand）服务。
 *
 * <p>按 sessionCode 聚合；demandCode(DM+10) 自动生成。budgetMin ≤ budgetMax 校验。
 */
public interface ServiceEquityDemandService {

    PageResult<ServiceEquityDemandVO> page(ServiceEquityDemandQueryDTO query);

    List<ServiceEquityDemandVO> listBySession(String sessionCode);

    ServiceEquityDemandVO getDetail(String demandCode);

    String create(ServiceEquityDemandCreateDTO dto);

    void update(String demandCode, ServiceEquityDemandUpdateDTO dto);

    void delete(String demandCode);
}
