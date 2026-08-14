package com.dayan.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 channel_open_sync_record 对应实体。
 *
 * <p>渠道开放平台同步记录（原 channel_data_sync_log，38 号迁移起更名，
 * 配套 {@code channel_open_platform} 记录所有同步操作）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("channel_open_sync_record")
public class ChannelOpenSyncRecord extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 同步记录编码 */
    private String syncCode;

    /** 渠道编码 */
    private String channelCode;

    /** 同步类型 */
    private Integer syncType;

    /** 业务编码 */
    private String bizCode;

    /** 方向 */
    private Integer direction;

    /** 请求报文 */
    private String requestData;

    /** 响应报文 */
    private String responseData;

    /** HTTP状态码 */
    private Integer httpStatus;

    /** 结果 */
    private Integer result;

    /** 错误信息 */
    private String errorMsg;

    /** 重试次数 */
    private Integer retryCount;

    /** 同步时间 */
    private LocalDateTime syncTime;
}
