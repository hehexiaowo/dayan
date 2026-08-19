package com.dayan.common.core.web;

import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.R;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 全局异常处理器回归测试：multipart 上传类异常必须返回明确的参数错误提示，
 * 不得落入兜底"系统内部异常"（历史上超限 PDF 上传即命中该问题）。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void maxUploadSizeExceededReturnsParamError() {
        R<Void> resp = handler.handleMaxUploadSize(new MaxUploadSizeExceededException(100L));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), resp.getCode());
        assertEquals("上传文件大小超过限制，请压缩后重试", resp.getMessage());
    }

    @Test
    void multipartParseFailureReturnsParamError() {
        R<Void> resp = handler.handleMultipart(new MultipartException("Could not parse multipart servlet request"));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), resp.getCode());
        assertEquals("上传文件解析失败，请检查文件后重试", resp.getMessage());
    }

    @Test
    void illegalArgumentReturnsParamError() {
        R<Void> resp = handler.handleIllegalArgument(new IllegalArgumentException("切块长度需在 1-6000 之间"));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), resp.getCode());
        assertEquals("切块长度需在 1-6000 之间", resp.getMessage());
    }
}
