package com.dayan.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.system.entity.SystemMessage;
import com.dayan.system.mapper.SystemMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 消息发送记录查询服务（system_message，只读审计）。
 *
 * <p>记录由各业务的消息发送链路落库（短信/站内信/推送/邮件统一收口），
 * 管理端仅提供分页与详情查询，用于排查发送失败与送达状态。
 */
@Service
@RequiredArgsConstructor
public class SystemMessageService {

    private final SystemMessageMapper messageMapper;

    /** 前端传入的时间参数格式（与 el-date-picker value-format 一致） */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 分页查询（业务类型/渠道/状态/接收者类型精确，模板编码模糊，时间范围过滤）。
     */
    public PageResult<SystemMessage> page(long current, long size,
                                          String bizType, Integer channelType, Integer sendStatus,
                                          String targetType, String templateCode,
                                          String startTime, String endTime) {
        LambdaQueryWrapper<SystemMessage> wrapper = new LambdaQueryWrapper<SystemMessage>()
                .orderByDesc(SystemMessage::getId);
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(SystemMessage::getBizType, bizType);
        }
        if (channelType != null) {
            wrapper.eq(SystemMessage::getChannelType, channelType);
        }
        if (sendStatus != null) {
            wrapper.eq(SystemMessage::getSendStatus, sendStatus);
        }
        if (targetType != null && !targetType.isEmpty()) {
            wrapper.eq(SystemMessage::getTargetType, targetType);
        }
        if (templateCode != null && !templateCode.isEmpty()) {
            wrapper.like(SystemMessage::getTemplateCode, templateCode);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(SystemMessage::getCreatedAt, LocalDateTime.parse(startTime, TIME_FORMATTER));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(SystemMessage::getCreatedAt, LocalDateTime.parse(endTime, TIME_FORMATTER));
        }
        Page<SystemMessage> page = messageMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(current, size, page.getTotal(), page.getRecords());
    }

    public SystemMessage getById(Long id) {
        SystemMessage message = messageMapper.selectById(id);
        if (message == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "消息记录不存在: id=" + id);
        }
        return message;
    }
}
