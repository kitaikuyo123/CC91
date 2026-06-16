package com.cc91.contentservice.config;

import com.cc91.contentservice.dto.ApiResponse;
import com.cc91.contentservice.exception.BadRequestException;
import com.cc91.contentservice.exception.ResourceNotFoundException;
import com.cc91.contentservice.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
     * 处理 Spring Security 的 @PreAuthorize 拒绝（403）
     * 必须在 RuntimeException 之前显式捕获，否则会被下面的
     * handleRuntimeException 当成普通 400 处理，掩盖正确的 403 响应。
     * 这修复了 ADMIN-only 端点对 USER 返回 400 而非 403 的问题。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Access denied by @PreAuthorize: {}", ex.getMessage());
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
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception: {}", ex.getMessage());
        return ResponseEntity.badRequest()
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
