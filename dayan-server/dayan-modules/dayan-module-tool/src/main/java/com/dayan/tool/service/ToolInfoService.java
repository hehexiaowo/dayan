package com.dayan.tool.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.tool.dto.ToolInfoCreateDTO;
import com.dayan.tool.dto.ToolInfoQueryDTO;
import com.dayan.tool.dto.ToolInfoUpdateDTO;
import com.dayan.tool.model.ToolAiartistPipelineConfig;
import com.dayan.tool.vo.ToolAichatPersonaVO;
import com.dayan.tool.vo.ToolAiartistConfigVO;
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
     * 端上工具列表：全部启用工具，按 id 升序。
     * 展示/路由细节由端上按 tool_type 固定映射，不在此过滤。
     */
    List<ToolInfoVO> listEnabled();

    /** 问答人物列表（tool_type=aichat 且启用，按 id 升序） */
    List<ToolAichatPersonaVO> listQaPersonas();

    /** 问答人物详情（tool_code 对应的 aichat 实例，不存在抛 NOT_FOUND） */
    ToolAichatPersonaVO getQaPersona(String toolCode);

    /** AI 创作分类列表（tool_type=aiartist 且启用，按 id 升序） */
    List<ToolAiartistConfigVO> listAiartistConfigs();

    /** AI 创作分类详情（tool_code 对应的 aiartist 实例，不存在抛 NOT_FOUND） */
    ToolAiartistConfigVO getAiartistConfig(String toolCode);

    /** AI 创作分类流水线配置（config_json.pipeline 全量解析，缺失回落内置默认值） */
    ToolAiartistPipelineConfig getAiartistPipelineConfig(String toolCode);

    String create(ToolInfoCreateDTO dto);

    void update(String toolCode, ToolInfoUpdateDTO dto);

    void delete(String toolCode);
}
