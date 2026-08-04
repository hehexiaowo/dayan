package com.dayan.common.core.resp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.slf4j.MDC;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 统一响应封装。
 *
 * <p>所有 Controller 出参统一使用 {@code R<T>}，由 {@code GlobalExceptionHandler} 兜底包装异常。
 *
 * @param <T> 业务数据类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 成功码 */
    public static final int CODE_SUCCESS = 0;
    /** 兜底系统异常码 */
    public static final int CODE_SYSTEM_ERROR = 10500;

    private int code;
    private String message;
    private T data;
    private Long timestamp;
    /** 链路追踪 ID（由 TraceIdFilter 写入 MDC） */
    private String traceId;

    public R() {
        this.timestamp = Instant.now().toEpochMilli();
        this.traceId = MDC.get("traceId");
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(CODE_SUCCESS);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> R<T> ok(T data, String message) {
        R<T> r = ok(data);
        r.setMessage(message);
        return r;
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public static <T> R<T> fail(int code, String message, T data) {
        R<T> r = fail(code, message);
        r.setData(data);
        return r;
    }

    public boolean success() {
        return this.code == CODE_SUCCESS;
    }
}
