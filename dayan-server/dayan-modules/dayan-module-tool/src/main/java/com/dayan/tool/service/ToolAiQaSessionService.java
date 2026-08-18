package com.dayan.tool.service;

import com.dayan.tool.vo.ToolAiQaSessionVO;
import java.util.List;

public interface ToolAiQaSessionService {
    /** 某人物下当前代理人的会话列表（时间倒序） */
    List<ToolAiQaSessionVO> listByPersona(String agentCode, Long configId);
    /** 新建会话（title 自动生成） */
    String create(String agentCode, String channelCode, Long configId);
    /** 删除会话（物理删消息 + 逻辑删会话） */
    void delete(String agentCode, String sessionCode);
}
