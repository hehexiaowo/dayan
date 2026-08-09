package com.dayan.service.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.service.dto.ArrangeConfirmDTO;
import com.dayan.service.dto.ServiceEquityArrangeCreateDTO;
import com.dayan.service.dto.ServiceEquityArrangeQueryDTO;
import com.dayan.service.dto.ServiceEquityArrangeUpdateDTO;
import com.dayan.service.vo.ServiceEquityArrangeVO;

import java.util.List;

/**
 * 全程安排（service_equity_arrange）服务。
 *
 * <p>按 sessionCode/solutionCode 聚合；arrangeCode(AR+10) 自动生成。
 * arrangeTimeStart < arrangeTimeEnd 校验；isConfirmed 置 1 后方可 start_service。
 */
public interface ServiceEquityArrangeService {

    PageResult<ServiceEquityArrangeVO> page(ServiceEquityArrangeQueryDTO query);

    List<ServiceEquityArrangeVO> listBySession(String sessionCode);

    ServiceEquityArrangeVO getDetail(String arrangeCode);

    String create(ServiceEquityArrangeCreateDTO dto);

    void update(String arrangeCode, ServiceEquityArrangeUpdateDTO dto);

    /** 确认安排（isConfirmed 置 1，写 confirmTime）。 */
    void confirm(ArrangeConfirmDTO dto);

    void delete(String arrangeCode);

    /** 统计会话下已确认安排数（start_service 前校验用）。 */
    long countByConfirmed(String sessionCode, Integer isConfirmed);
}
