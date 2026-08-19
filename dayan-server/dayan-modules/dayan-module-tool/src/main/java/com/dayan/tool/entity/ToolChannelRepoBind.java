package com.dayan.tool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 tool_channel_repo_bind 对应实体（渠道问答人物补充知识库绑定）。
 *
 * <p>你问我答人物知识库两层模型：admin 全局绑定存 tool_info.config_json.repoIds，
 * 渠道补充存本表；运行时有效库 = 全局 ∪ 渠道补充（并集去重）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_channel_repo_bind")
public class ToolChannelRepoBind extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 问答人物编码（tool_info.tool_code，TL 前缀） */
    private String toolCode;

    /** 补充方渠道编码（服务端 ContextHolder 注入） */
    private String channelCode;

    /** 补充的知识库 ID（system_knowledge_repo.id） */
    private Long repoId;
}
