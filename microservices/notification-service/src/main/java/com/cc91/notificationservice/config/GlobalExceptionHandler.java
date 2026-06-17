package com.cc91.notificationservice.config;

import com.cc91.notificationservice.dto.ApiResponse;
import com.cc91.notificationservice.exception.BadRequestException;
import com.cc91.notificationservice.exception.ResourceNotFoundException;
import com.cc91.notificationservice.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<String>> handleUnauthorizedException(UnauthorizedException ex) {
        logger.warn("Unauthorized access: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<String>> handleBadRequestException(BadRequestException ex) {
        logger.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(ex.getMessage()));
    }

    /**
     * 处理 Spring Security 的 AccessDeniedException（403）
     * 必须在 RuntimeException 之前显式捕获，否则会被下面的
     * handleRuntimeException 当成普通 400 处理。当前本服务虽未使用 @PreAuthorize，
     * 但为防御未来引入的鉴权逻辑、避免 OWASP A01 鉴权混淆，显式声明。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>("Access denied"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(ex.getMessage()));
    }

    /**
     * 处理 Spring 6 方法级参数校验失败（400）
     * <p>
     * Controller 上 @Validated + @RequestParam 校验失败（如 NotificationController
     * 的 page/size 参数边界校验）会抛此异常。该异常并非服务器 bug，而是客户端
     * 输入错误，必须返回 400，否则会被 RuntimeException 兜底误判为 500。
     * 必须声明在 RuntimeException 之前以保证优先匹配。
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<String>> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex) {
        logger.warn("Method validation failed: {}", ex.getMessage());
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>("服务器内部错误"));
    }
}
