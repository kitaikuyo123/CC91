package com.cc91.userservice.config;

import com.cc91.userservice.dto.ApiResponse;
import com.cc91.userservice.exception.BadRequestException;
import com.cc91.userservice.exception.ResourceNotFoundException;
import com.cc91.userservice.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        logger.warn("Validation failed: {}", errors);
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>("输入验证失败", errors));
    }

    /**
     * 处理资源不存在异常（404）
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(ex.getMessage()));
    }

    /**
     * 处理无权限操作异常（403）
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<String>> handleUnauthorizedException(UnauthorizedException ex) {
        logger.warn("Unauthorized access: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(ex.getMessage()));
    }

    /**
     * 处理 Spring Security 访问拒绝异常（403）
     * <p>
     * Method-security @PreAuthorize 拒绝抛出的 AccessDeniedException 会被
     * @RestControllerAdvice 截获（在 ExceptionTranslationFilter 之前），
     * 必须显式返回 403，否则会被下面的 RuntimeException 处理器误转为 400。
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>("Access denied"));
    }

    /**
     * 处理请求参数错误异常（400）
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<String>> handleBadRequestException(BadRequestException ex) {
        logger.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(ex.getMessage()));
    }

    /**
     * 处理运行时异常（500 兜底）
     * 未预期的 RuntimeException 应返回 500 而非 400，避免把服务端 bug 伪装成客户端错误。
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(RuntimeException ex) {
        logger.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(ex.getMessage()));
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>("服务器内部错误"));
    }
}
