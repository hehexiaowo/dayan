package com.dayan.service.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.service.dto.ServiceEquitySolutionCreateDTO;
import com.dayan.service.dto.ServiceEquitySolutionQueryDTO;
import com.dayan.service.dto.ServiceEquitySolutionUpdateDTO;
import com.dayan.service.dto.SolutionAcceptDTO;
import com.dayan.service.vo.ServiceEquitySolutionVO;

import java.util.List;

/**
 * 方案定制（service_equity_solution）服务。
 *
 * <p>按 sessionCode 聚合；多方案（推荐/备选）。solutionCode(SO+10) 自动生成。
 * 确认方案会话前须存在 isAccepted=1 的方案；驳回时 adjustCount+1。
 */
public interface ServiceEquitySolutionService {

    PageResult<ServiceEquitySolutionVO> page(ServiceEquitySolutionQueryDTO query);

    List<ServiceEquitySolutionVO> listBySession(String sessionCode);

    ServiceEquitySolutionVO getDetail(String solutionCode);

    String create(ServiceEquitySolutionCreateDTO dto);

    void update(String solutionCode, ServiceEquitySolutionUpdateDTO dto);

    /** 标记方案接受/拒绝（isAccepted: 0/1/2）。 */
    void accept(SolutionAcceptDTO dto);

    void delete(String solutionCode);

    /** 统计会话下指定接受标记的方案数（confirm_solution 前校验用）。 */
    long countByAccepted(String sessionCode, Integer isAccepted);
}
