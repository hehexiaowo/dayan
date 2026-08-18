package com.dayan.tool.service.impl;

import cn.hutool.core.util.StrUtil;
import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.tool.dto.ToolCalculatorRecordCreateDTO;
import com.dayan.tool.entity.ToolGapcalRecord;
import com.dayan.tool.entity.ToolPensioncalRecord;
import com.dayan.tool.mapper.ToolGapcalRecordMapper;
import com.dayan.tool.mapper.ToolPensioncalRecordMapper;
import com.dayan.tool.service.ToolCalculatorRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ToolCalculatorRecordServiceImpl implements ToolCalculatorRecordService {

    private final ToolPensioncalRecordMapper pensionMapper;
    private final ToolGapcalRecordMapper gapMapper;
    private final CodeGenerator codeGenerator;

    @Override
    public String savePension(ToolCalculatorRecordCreateDTO dto) {
        ToolPensioncalRecord record = new ToolPensioncalRecord();
        record.setRecordCode(codeGenerator.generate("TPC"));
        record.setToolCode(StrUtil.blankToDefault(dto.getToolCode(), "TL00001"));
        record.setAgentCode(requireAgentCode());
        record.setChannelCode(StrUtil.nullToEmpty(ContextHolder.getChannelCode()));
        record.setInputJson(dto.getInputJson());
        record.setResultJson(dto.getResultJson());
        pensionMapper.insert(record);
        return record.getRecordCode();
    }

    @Override
    public String saveGap(ToolCalculatorRecordCreateDTO dto) {
        ToolGapcalRecord record = new ToolGapcalRecord();
        record.setRecordCode(codeGenerator.generate("TGC"));
        record.setToolCode(StrUtil.blankToDefault(dto.getToolCode(), "TL00002"));
        record.setAgentCode(requireAgentCode());
        record.setChannelCode(StrUtil.nullToEmpty(ContextHolder.getChannelCode()));
        record.setInputJson(dto.getInputJson());
        record.setResultJson(dto.getResultJson());
        gapMapper.insert(record);
        return record.getRecordCode();
    }

    private String requireAgentCode() {
        String agentCode = ContextHolder.getAccountCode();
        if (StrUtil.isBlank(agentCode)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return agentCode;
    }
}
