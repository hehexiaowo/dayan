package com.dayan.tool.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.tool.dto.ToolInfoCreateDTO;
import com.dayan.tool.dto.ToolInfoQueryDTO;
import com.dayan.tool.dto.ToolInfoUpdateDTO;
import com.dayan.tool.vo.ToolInfoVO;

import java.util.List;

/**
 * 工具服务。
 *
 * <p>{@code toolCode} 由系统生成（TL + 5 位序列），全表唯一。
 * {@code tool_info} 为平台共享表（无 channel_code），不受渠道字段隔离约束。
 */
public interface ToolInfoService {

    PageResult<ToolInfoVO> page(ToolInfoQueryDTO query);

    List<ToolInfoVO> list(ToolInfoQueryDTO query);

    ToolInfoVO getDetail(String toolCode);

    /**
     * 端上工具列表：启用且 visibleScope 含指定端标识，按 sortOrder 升序。
     *
     * @param end 端标识（agent/client）
     */
    List<ToolInfoVO> listForEnd(String end);

    String create(ToolInfoCreateDTO dto);

    void update(String toolCode, ToolInfoUpdateDTO dto);

    void delete(String toolCode);
}
