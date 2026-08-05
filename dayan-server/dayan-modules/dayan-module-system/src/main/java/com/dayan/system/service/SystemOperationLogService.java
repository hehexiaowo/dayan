package com.dayan.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.resp.PageResult;
import com.dayan.system.entity.SystemOperationLog;
import com.dayan.system.mapper.SystemOperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 操作日志查询服务。
 *
 * <p>负责 system_operation_log 表的分页查询，供 Admin 端审计页展示。
 * 写入由 {@code SystemOperationLogPublisher}（@OperationLog 切面异步触发）完成，
 * 本服务只读。
 */
@Service
@RequiredArgsConstructor
public class SystemOperationLogService {

    private final SystemOperationLogMapper operationLogMapper;

    /** 前端传入的时间参数格式（与 el-date-picker value-format 一致） */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 分页查询（按 module 模糊 / accountCode 精确 / resultStatus 精确 / 时间范围 过滤）。
     *
     * @param current      页码（从 1 开始）
     * @param size         每页条数
     * @param module       操作模块（模糊匹配）
     * @param accountCode  操作账号编码（精确匹配）
     * @param resultStatus 结果状态：1=成功 / 0=失败
     * @param startTime    起始时间（ISO 格式 yyyy-MM-ddTHH:mm:ss）
     * @param endTime      结束时间（ISO 格式 yyyy-MM-ddTHH:mm:ss）
     */
    public PageResult<SystemOperationLog> page(long current, long size,
                                               String module, String accountCode,
                                               Integer resultStatus,
                                               String startTime, String endTime) {
        LambdaQueryWrapper<SystemOperationLog> wrapper = new LambdaQueryWrapper<SystemOperationLog>()
                .orderByDesc(SystemOperationLog::getCreatedAt);
        if (module != null && !module.isEmpty()) {
            wrapper.like(SystemOperationLog::getModule, module);
        }
        if (accountCode != null && !accountCode.isEmpty()) {
            wrapper.eq(SystemOperationLog::getAccountCode, accountCode);
        }
        if (resultStatus != null) {
            wrapper.eq(SystemOperationLog::getResultStatus, resultStatus);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(SystemOperationLog::getCreatedAt, LocalDateTime.parse(startTime, TIME_FORMATTER));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(SystemOperationLog::getCreatedAt, LocalDateTime.parse(endTime, TIME_FORMATTER));
        }
        Page<SystemOperationLog> page = operationLogMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(current, size, page.getTotal(), page.getRecords());
    }

    /** 按主键查询单条日志详情 */
    public SystemOperationLog getById(Long id) {
        return operationLogMapper.selectById(id);
    }
}
