package com.dayan.butler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 butler_client_rel 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("butler_client_rel")
public class ButlerClientRel extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 管家编码 */
    private String butlerCode;

    /** 客户编码 */
    private String clientCode;

    /** 绑定时间 */
    private LocalDateTime bindTime;

    /** 状态 */
    private Integer status;
}
