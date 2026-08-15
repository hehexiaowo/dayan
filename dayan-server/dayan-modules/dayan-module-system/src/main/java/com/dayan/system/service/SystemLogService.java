package com.dayan.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.resp.PageResult;
import com.dayan.system.entity.SystemLogEntry;
import com.dayan.system.enums.SystemLogSource;
import com.dayan.system.log.SystemLogRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 系统日志查询服务（四端分表，按 source 路由）。
 *
 * <p>写入由 {@code SystemLogPublisher}（@OperationLog 切面）与
 * {@code SystemAuthLogRecorder}（登录/登出）完成，本服务只读。
 * 由于四端分表且各表自增主键可能重复，分页与详情都必须携带 source。
 */
@Service
@RequiredArgsConstructor
public class SystemLogService {

    private final SystemLogRouter router;

    /** 前端传入的时间参数格式（与 el-date-picker value-format 一致） */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 分页查询（按 module 模糊 / accountCode 精确 / resultStatus 精确 / 时间范围 过滤）。
     *
     * @param source       日志来源：organ/channel/agent/client（非法值兜底 organ）
     * @param current      页码（从 1 开始）
     * @param size         每页条数
     * @param module       操作模块（模糊匹配；auth=登录登出）
     * @param accountCode  操作账号编码（精确匹配）
     * @param resultStatus 结果状态：1=成功 / 0=失败
     * @param startTime    起始时间（ISO 格式 yyyy-MM-ddTHH:mm:ss）
     * @param endTime      结束时间（ISO 格式 yyyy-MM-ddTHH:mm:ss）
     */
    public PageResult<? extends SystemLogEntry> page(String source, long current, long size,
                                                     String module, String accountCode,
                                                     Integer resultStatus,
                                                     String startTime, String endTime) {
        SystemLogSource src = SystemLogSource.of(source);
        return pageTyped(src, current, size, module, accountCode, resultStatus, startTime, endTime);
    }

    /** 按来源 + 主键查询单条日志详情 */
    public SystemLogEntry getById(String source, Long id) {
        SystemLogSource src = SystemLogSource.of(source);
        return router.mapperOf(src).selectById(id);
    }

    @SuppressWarnings("unchecked")
    private <T extends SystemLogEntry> PageResult<T> pageTyped(SystemLogSource src, long current, long size,
                                                               String module, String accountCode,
                                                               Integer resultStatus,
                                                               String startTime, String endTime) {
        BaseMapper<T> mapper = (BaseMapper<T>) router.mapperOf(src);
        // 显式指定子类实体类：基类 SystemLogEntry 无 Mapper 注册，
        // lambda 列缓存必须借具体表实体的 TableInfo 解析（见 SystemLogRouter.entityClassOf）
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>((Class<T>) router.entityClassOf(src))
                .orderByDesc(SystemLogEntry::getCreatedAt);
        if (module != null && !module.isEmpty()) {
            wrapper.like(SystemLogEntry::getModule, module);
        }
        if (accountCode != null && !accountCode.isEmpty()) {
            wrapper.eq(SystemLogEntry::getAccountCode, accountCode);
        }
        if (resultStatus != null) {
            wrapper.eq(SystemLogEntry::getResultStatus, resultStatus);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(SystemLogEntry::getCreatedAt, LocalDateTime.parse(startTime, TIME_FORMATTER));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(SystemLogEntry::getCreatedAt, LocalDateTime.parse(endTime, TIME_FORMATTER));
        }
        Page<T> page = mapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(current, size, page.getTotal(), page.getRecords());
    }
}
