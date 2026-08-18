package com.dayan.tool.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.tool.dto.ToolAiQaConfigCreateDTO;
import com.dayan.tool.dto.ToolAiQaConfigQueryDTO;
import com.dayan.tool.dto.ToolAiQaConfigUpdateDTO;
import com.dayan.tool.vo.ToolAiQaConfigVO;

import java.util.List;

public interface ToolAiQaConfigService {
    PageResult<ToolAiQaConfigVO> page(ToolAiQaConfigQueryDTO query);

    List<ToolAiQaConfigVO> listEnabled();

    ToolAiQaConfigVO getDetail(Long id);

    String create(ToolAiQaConfigCreateDTO dto);

    void update(Long id, ToolAiQaConfigUpdateDTO dto);

    void delete(Long id);
}
