package com.dayan.tool.service;

import com.dayan.tool.vo.ToolAiQaSessionVO;
import java.util.List;

public interface ToolAiQaSessionService {
    /** 某人物下当前代理人的会话列表（时间倒序） */
    List<ToolAiQaSessionVO> listByPersona(String agentCode, Long configId);
    /** 新建会话（title 自动生成；toolCode 为空回落 TL00004） */
    String create(String agentCode, String channelCode, Long configId, String toolCode);
    /** 删除会话（物理删消息 + 逻辑删会话） */
    void delete(String agentCode, String sessionCode);
}
