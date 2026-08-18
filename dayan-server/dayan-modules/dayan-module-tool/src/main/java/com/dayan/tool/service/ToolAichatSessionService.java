package com.dayan.tool.service;

import com.dayan.tool.vo.ToolAichatSessionVO;
import java.util.List;

public interface ToolAichatSessionService {
    /** 某问答人物（toolCode）下当前代理人的会话列表（时间倒序） */
    List<ToolAichatSessionVO> listByTool(String agentCode, String toolCode);
    /** 新建会话（title 自动生成，personaName 冗余自 tool_info.tool_name） */
    String create(String agentCode, String channelCode, String toolCode);
    /** 删除会话（物理删消息 + 逻辑删会话） */
    void delete(String agentCode, String sessionCode);
}
