package com.dayan.service.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.service.dto.ServiceEvaluationCreateDTO;
import com.dayan.service.dto.ServiceEvaluationQueryDTO;
import com.dayan.service.dto.ServiceEvaluationUpdateDTO;
import com.dayan.service.vo.ServiceEvaluationVO;

import java.util.List;

/**
 * 服务评价（service_evaluation）服务。
 *
 * <p>按 sessionCode 聚合；<b>一会话一评价</b>（同 sessionCode 仅允许 1 条评价，由应用层校验）。
 * 4 维评分（attitudeRating/professionalRating/responsivenessRating/satisfactionRating，1-5）。
 */
public interface ServiceEvaluationService {

    PageResult<ServiceEvaluationVO> page(ServiceEvaluationQueryDTO query);

    /**
     * 按条件查询评价列表（支持 sessionCode/butlerCode/parkCode/clientCode 等过滤）。
     * 一会话一评价，故按 sessionCode 过滤至多返回 1 条。
     */
    List<ServiceEvaluationVO> list(ServiceEvaluationQueryDTO query);

    ServiceEvaluationVO getDetail(Long id);

    /**
     * 新建评价。同 sessionCode 已存在评价时抛 {@code BusinessException("该服务会话已存在评价")}。
     *
     * @return 新建评价主键 id
     */
    Long create(ServiceEvaluationCreateDTO dto);

    void update(Long id, ServiceEvaluationUpdateDTO dto);

    void delete(Long id);
}
