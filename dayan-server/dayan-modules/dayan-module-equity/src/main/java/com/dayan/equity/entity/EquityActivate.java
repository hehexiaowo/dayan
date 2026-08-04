package com.dayan.equity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 equity_activate 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equity_activate")
public class EquityActivate extends BaseEntity {

    /** 主键（分片表，雪花ID，MyBatis-Plus 自动分配） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 激活记录编码 */
    private String activateCode;

    /** 权益编码 */
    private String equityCode;

    /** 权益模板编码 */
    private String templateCode;

    /** 激活客户编码 */
    private String clientCode;

    /** 激活客户姓名(快照) */
    private String clientFullName;

    /** 激活客户手机号 */
    private String clientPhone;

    /** 激活渠道 */
    private Integer activateChannel;

    /** 激活来源编码 */
    private String activateSourceCode;

    /** 激活时间 */
    private LocalDateTime activateTime;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 是否实名认证 */
    private Integer isIdCardVerified;

    /** 是否签署协议 */
    private Integer isAgreementSigned;

    /** 激活IP */
    private String ipAddress;

    /** 设备信息 */
    private String deviceInfo;

    /** 备注 */
    private String remark;
}
