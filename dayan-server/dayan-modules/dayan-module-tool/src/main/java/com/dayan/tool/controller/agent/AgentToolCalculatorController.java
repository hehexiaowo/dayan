package com.dayan.tool.controller.agent;

import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.tool.dto.ToolCalculatorRecordCreateDTO;
import com.dayan.tool.service.ToolCalculatorRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agent 计算器记录")
@RestController
@RequestMapping("/tools/calculator")
@RequiredArgsConstructor
public class AgentToolCalculatorController {

    private final ToolCalculatorRecordService calculatorRecordService;

    @Operation(summary = "保存社保养老计算器记录")
    @OperationLog(module = "工具配置", action = "社保养老计算")
    @PostMapping("/pension")
    public R<String> savePension(@RequestBody @Valid ToolCalculatorRecordCreateDTO dto) {
        return R.ok(calculatorRecordService.savePension(dto));
    }

    @Operation(summary = "保存养老缺口计算器记录")
    @OperationLog(module = "工具配置", action = "养老缺口计算")
    @PostMapping("/gap")
    public R<String> saveGap(@RequestBody @Valid ToolCalculatorRecordCreateDTO dto) {
        return R.ok(calculatorRecordService.saveGap(dto));
    }
}
