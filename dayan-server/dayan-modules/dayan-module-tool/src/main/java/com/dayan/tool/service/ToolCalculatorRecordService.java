package com.dayan.tool.service;

import com.dayan.tool.dto.ToolCalculatorRecordCreateDTO;

/**
 * 两个计算器使用记录。
 */
public interface ToolCalculatorRecordService {

    String savePension(ToolCalculatorRecordCreateDTO dto);

    String saveGap(ToolCalculatorRecordCreateDTO dto);
}
