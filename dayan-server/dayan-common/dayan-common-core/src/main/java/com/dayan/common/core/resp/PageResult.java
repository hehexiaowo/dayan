package com.dayan.common.core.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页响应封装。
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前页码（从 1 开始） */
    private long current;
    /** 每页大小 */
    private long size;
    /** 总记录数 */
    private long total;
    /** 总页数 */
    private long pages;
    /** 当前页数据 */
    private List<T> records;

    public PageResult() {
    }

    public PageResult(long current, long size, long total, List<T> records) {
        this.current = current;
        this.size = size;
        this.total = total;
        this.pages = size > 0 ? (total + size - 1) / size : 0;
        this.records = records == null ? Collections.emptyList() : records;
    }

    /**
     * 空结果。
     */
    public static <T> PageResult<T> empty(long current, long size) {
        return new PageResult<>(current, size, 0, Collections.emptyList());
    }

    /**
     * 列表元素类型转换（Entity → VO）。
     */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        List<R> mapped = this.records.stream().map(mapper).collect(Collectors.toList());
        return new PageResult<>(this.current, this.size, this.total, mapped);
    }
}
