package com.tastelink.exception;

import com.tastelink.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常：携带业务码与 HTTP 状态，由 GlobalExceptionHandler 统一转换。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final int httpStatus;

    public BusinessException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
        this.httpStatus = rc.getHttpStatus();
    }

    public BusinessException(ResultCode rc, String message) {
        super(message != null ? message : rc.getMessage());
        this.code = rc.getCode();
        this.httpStatus = rc.getHttpStatus();
    }
}
