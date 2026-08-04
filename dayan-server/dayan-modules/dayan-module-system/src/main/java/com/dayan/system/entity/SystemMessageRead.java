package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 system_message_read 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_message_read")
public class SystemMessageRead extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 消息编码 */
    private String messageCode;

    /** 账号类型 */
    private String accountType;

    /** 接收者编码 */
    private String accountCode;

    /** 是否已读 */
    private Integer isRead;

    /** 阅读时间 */
    private LocalDateTime readTime;
}
