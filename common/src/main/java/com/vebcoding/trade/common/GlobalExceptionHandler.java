package com.vebcoding.trade.common;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {
        return response(ErrorCode.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return response(exception.code(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("请求参数不正确");
        return response(ErrorCode.INVALID_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableRequest() {
        return response(ErrorCode.INVALID_REQUEST, "请求内容格式错误，请检查字段类型和 JSON 格式");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException exception) {
        return response(ErrorCode.INVALID_REQUEST, "缺少必填参数：" + exception.getParameterName());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException exception) {
        return response(ErrorCode.INVALID_REQUEST, "缺少必要请求头：" + exception.getHeaderName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return response(ErrorCode.INVALID_REQUEST, "参数 " + exception.getName() + " 的格式不正确");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return response(ErrorCode.METHOD_NOT_ALLOWED, "当前接口不支持 " + exception.getMethod() + " 请求");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound() {
        return response(ErrorCode.NOT_FOUND, "请求的接口或资源不存在");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadTooLarge() {
        return response(ErrorCode.PAYLOAD_TOO_LARGE, "上传文件超过系统允许的大小");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataConflict() {
        return response(ErrorCode.CONFLICT, "数据与现有记录冲突，或仍被其他业务引用");
    }

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseUnavailable() {
        return response(ErrorCode.SERVICE_UNAVAILABLE, "数据库暂时无法连接，请稍后重试");
    }

    @ExceptionHandler(QueryTimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseTimeout() {
        return response(ErrorCode.REQUEST_TIMEOUT, "数据库处理超时，请缩小查询范围后重试");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        String reference = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LOGGER.error("Unhandled request error, reference={}", reference, exception);
        return response(ErrorCode.INTERNAL_ERROR, "系统处理请求时发生未识别错误，请联系管理员并提供参考编号：" + reference);
    }

    private ResponseEntity<ApiResponse<Void>> response(ErrorCode code, String message) {
        return ResponseEntity.status(HttpStatus.valueOf(code.status())).body(ApiResponse.fail(code, message));
    }
}
