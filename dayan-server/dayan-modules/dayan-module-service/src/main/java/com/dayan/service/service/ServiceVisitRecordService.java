package com.dayan.service.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.service.dto.ServiceVisitRecordCreateDTO;
import com.dayan.service.dto.ServiceVisitRecordQueryDTO;
import com.dayan.service.dto.ServiceVisitRecordUpdateDTO;
import com.dayan.service.vo.ServiceVisitRecordVO;

import java.util.List;

/**
 * 探访记录（service_visit_record）服务。
 *
 * <p>按 butlerCode/parkCode 聚合；overallScore 综合评分；6 项检查（facility/service/hygiene/food/safety
 * 文本 + issuesFound）。
 */
public interface ServiceVisitRecordService {

    PageResult<ServiceVisitRecordVO> page(ServiceVisitRecordQueryDTO query);

    /**
     * 按条件查询探访记录列表（支持 butlerCode/parkCode/visitPurpose 等过滤）。
     */
    List<ServiceVisitRecordVO> list(ServiceVisitRecordQueryDTO query);

    ServiceVisitRecordVO getDetail(Long id);

    /**
     * 新建探访记录。
     *
     * @return 新建记录主键 id
     */
    Long create(ServiceVisitRecordCreateDTO dto);

    void update(Long id, ServiceVisitRecordUpdateDTO dto);

    void delete(Long id);
}
