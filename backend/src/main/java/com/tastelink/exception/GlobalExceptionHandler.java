package com.tastelink.exception;

import com.tastelink.common.R;
import com.tastelink.common.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：统一返回 R，设置对应 HTTP 状态；不向前端泄露 SQL/堆栈。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按异常携带的 httpStatus 输出。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusiness(BusinessException ex) {
        log.warn("business exception: code={}, message={}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(R.fail(ex.getCode(), ex.getMessage()));
    }

    /** @Valid 请求体校验失败。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleBodyInvalid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse(ResultCode.BAD_REQUEST.getMessage());
        log.warn("validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(R.fail(ResultCode.BAD_REQUEST, message));
    }

    /** 表单/参数绑定校验失败。 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBind(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse(ResultCode.BAD_REQUEST.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(R.fail(ResultCode.BAD_REQUEST, message));
    }

    /** @Validated 方法参数/路径变量校验失败。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraint(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .orElse(ResultCode.BAD_REQUEST.getMessage());
        log.warn("constraint violation: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(R.fail(ResultCode.BAD_REQUEST, message));
    }

    /** 参数类型转换失败（如 path 变量非数字）。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(R.fail(ResultCode.BAD_REQUEST, "参数格式错误"));
    }

    /** 唯一约束冲突（兜底，正常幂等场景已在 Service 层捕获）。 */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<R<Void>> handleDuplicate(DuplicateKeyException ex) {
        log.warn("duplicate key: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(R.fail(ResultCode.ALREADY_LIKED.getCode(), "操作冲突，请重试"));
    }

    /** 未认证（兜底；JWT 缺失/失效主要由 AuthenticationEntryPoint 处理）。 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<R<Void>> handleUnauthenticated(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(R.fail(ResultCode.UNAUTHORIZED));
    }

    /** 鉴权通过但越权（如改他人资料）。 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(R.fail(ResultCode.FORBIDDEN));
    }

    /** 路由不匹配 -> 404。 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Void>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(R.fail(ResultCode.NOT_FOUND));
    }

    /** 兜底：其余未捕获异常 -> 500，不泄露堆栈。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleAny(Exception ex) {
        log.error("unexpected exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail(ResultCode.SERVER_ERROR));
    }
}
