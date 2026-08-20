package com.dayan.channel.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 渠道保存问答人物补充知识库请求（全量替换；空数组 = 清空补充） */
@Data
public class ChannelToolAichatRepoBindDTO {

    @Size(max = 100, message = "补充知识库数量过多")
    private List<Long> repoIds;
}
