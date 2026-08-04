package com.dayan.equity.service;

import com.dayan.equity.dto.EquityTemplateCreateDTO;
import com.dayan.equity.dto.EquityTemplateQueryDTO;
import com.dayan.equity.dto.EquityTemplateUpdateDTO;
import com.dayan.equity.entity.EquityTemplate;
import com.dayan.equity.vo.EquityTemplateVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 权益模板服务。
 *
 * <p>平台共享表（{@code IdType.AUTO}），Admin 端全局管理。{@code template_code}(ET+5) 唯一。
 */
public interface EquityTemplateService {

    PageResult<EquityTemplateVO> page(EquityTemplateQueryDTO query);

    List<EquityTemplateVO> list(EquityTemplateQueryDTO query);

    EquityTemplateVO getDetail(String templateCode);

    /**
     * 按编码查询实体（供 depot 链路冗余字段使用）。不存在抛业务异常。
     */
    EquityTemplate requireTemplate(String templateCode);

    String create(EquityTemplateCreateDTO dto);

    void update(String templateCode, EquityTemplateUpdateDTO dto);

    void delete(String templateCode);
}
