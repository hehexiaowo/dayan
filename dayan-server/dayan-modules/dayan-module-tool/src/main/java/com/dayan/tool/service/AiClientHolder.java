package com.dayan.tool.service;

import cn.hutool.core.util.StrUtil;
import com.dayan.common.aliyun.bailian.BailianChatClient;
import com.dayan.common.aliyun.dashscope.DashScopeImageClient;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.system.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AI 客户端持有者：百炼对话 + 文生图客户端单实例，llm.* 配置统一出口。
 * 客户端方法级传配置（无状态），此处仅收敛 system_config 读取与默认值。
 */
@Service
@RequiredArgsConstructor
public class AiClientHolder {

    private final SystemConfigService systemConfigService;

    private final BailianChatClient chatClient = new BailianChatClient();
    private final DashScopeImageClient imageClient = new DashScopeImageClient();

    public BailianChatClient chatClient() {
        return chatClient;
    }

    public DashScopeImageClient imageClient() {
        return imageClient;
    }

    public String requireConfig(String key, String message) {
        String value = getConfig(key);
        if (StrUtil.isBlank(value)) {
            throw new BusinessException(ErrorCode.BUSINESS, message);
        }
        return value;
    }

    public String getConfig(String key) {
        return systemConfigService.getValue("llm", key);
    }

    public String chatModel() {
        return StrUtil.blankToDefault(getConfig("chat-model"), "qwen-plus");
    }
}
