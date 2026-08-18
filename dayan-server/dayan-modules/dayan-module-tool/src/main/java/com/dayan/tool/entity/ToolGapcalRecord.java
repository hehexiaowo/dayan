package com.dayan.tool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 养老缺口计算器使用记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_gap_calculator_record")
public class ToolGapCalculatorRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String recordCode;

    private String toolCode;

    private String agentCode;

    private String channelCode;

    private String inputJson;

    private String resultJson;
}
