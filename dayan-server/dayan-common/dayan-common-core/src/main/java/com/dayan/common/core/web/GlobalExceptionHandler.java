package com.dayan.common.core.web;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.exception.ParamException;
import com.dayan.common.core.resp.R;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>将各类异常统一转换为 {@link R} 响应，避免堆栈直接暴露给前端。
 * 处理顺序按异常具体程度排列。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：透传错误码与消息 */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验异常：@Valid body 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleArgNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /** 参数校验异常：表单绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", msg);
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /** 参数校验异常：@RequestParam/@PathVariable 校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("约束校验失败: {}", msg);
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /** 缺少必填参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        String msg = "缺少必填参数: " + e.getParameterName();
        log.warn(msg);
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /** 请求体不可读（JSON 格式错误） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), "请求体格式错误");
    }

    /** 请求方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), "请求方法不支持: " + e.getMethod());
    }

    /** 路由不存在 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public R<Void> handleNoHandler(NoHandlerFoundException e) {
        return R.fail(ErrorCode.NOT_FOUND.getCode(), "接口不存在: " + e.getRequestURL());
    }

    /** 手动抛出的参数异常 */
    @ExceptionHandler(ParamException.class)
    public R<Void> handleParam(ParamException e) {
        log.warn("参数异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /** 非法参数（如配置校验 IllegalArgumentException），返回参数错误而非兜底系统异常 */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数非法: {}", e.getMessage());
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 上传文件超过大小限制。
     *
     * <p>multipart 在 controller 方法进入前解析，超限异常（如 max-file-size）
     * 若不做专门处理会落入下方兜底返回"系统内部异常"，误导排查。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过大小限制: {}", e.getMessage());
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), "上传文件大小超过限制，请压缩后重试");
    }

    /** multipart 解析失败（请求体损坏、参数缺失等） */
    @ExceptionHandler(MultipartException.class)
    public R<Void> handleMultipart(MultipartException e) {
        log.warn("上传请求解析失败: {}", e.getMessage());
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), "上传文件解析失败，请检查文件后重试");
    }

    /** 兜底系统异常 */
    @ExceptionHandler(Exception.class)
    public R<Void> handleSystem(Exception e) {
        log.error("系统异常", e);
        return R.fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }
}
