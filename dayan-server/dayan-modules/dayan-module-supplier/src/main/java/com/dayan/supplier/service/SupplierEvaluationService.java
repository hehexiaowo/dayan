package com.dayan.supplier.service;

import com.dayan.supplier.dto.SupplierEvaluationCreateDTO;
import com.dayan.supplier.dto.SupplierEvaluationQueryDTO;
import com.dayan.supplier.dto.SupplierEvaluationUpdateDTO;
import com.dayan.supplier.vo.SupplierEvaluationVO;
import com.dayan.common.core.resp.PageResult;

/**
 * 供应商评估服务。
 *
 * <p>4 维评分（服务质量/设施质量/配合度/投诉率），{@code totalScore} 按公式自动计算，
 * {@code scoreLevel} 按总分 A/B/C/D 分级。
 */
public interface SupplierEvaluationService {

    PageResult<SupplierEvaluationVO> page(SupplierEvaluationQueryDTO query);

    SupplierEvaluationVO getDetail(Long id);

    Long create(SupplierEvaluationCreateDTO dto);

    void update(Long id, SupplierEvaluationUpdateDTO dto);

    void delete(Long id);
}
