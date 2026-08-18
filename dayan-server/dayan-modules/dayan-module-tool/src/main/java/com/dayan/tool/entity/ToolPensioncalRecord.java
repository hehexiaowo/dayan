package com.dayan.tool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 社保养老计算器使用记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_pensioncal_record")
public class ToolPensioncalRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String recordCode;

    private String toolCode;

    private String agentCode;

    private String channelCode;

    private String inputJson;

    private String resultJson;
}
