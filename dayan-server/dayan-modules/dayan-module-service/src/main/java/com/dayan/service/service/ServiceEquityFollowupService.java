package com.dayan.service.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.service.dto.ServiceEquityFollowupCreateDTO;
import com.dayan.service.dto.ServiceEquityFollowupQueryDTO;
import com.dayan.service.dto.ServiceEquityFollowupUpdateDTO;
import com.dayan.service.vo.ServiceEquityFollowupVO;

import java.util.List;

/**
 * 回访品控（service_equity_followup）服务。
 *
 * <p>按 sessionCode/arrangeCode 聚合；followupCode(FU+10) 自动生成。
 * 4 维满意度 1-5；任一 <3 时 isFollowupNeeded=1 + nextFollowupDate。
 */
public interface ServiceEquityFollowupService {

    PageResult<ServiceEquityFollowupVO> page(ServiceEquityFollowupQueryDTO query);

    List<ServiceEquityFollowupVO> listBySession(String sessionCode);

    ServiceEquityFollowupVO getDetail(Long id);

    String create(ServiceEquityFollowupCreateDTO dto);

    void update(Long id, ServiceEquityFollowupUpdateDTO dto);

    void delete(Long id);
}
