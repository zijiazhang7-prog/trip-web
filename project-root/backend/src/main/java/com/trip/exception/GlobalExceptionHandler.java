package com.trip.exception;

import com.trip.common.ApiResponse;
import com.trip.common.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理，统一转换为 ApiResponse。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity
                .status(statusOf(exception.getErrorCode()))
                .body(ApiResponse.fail(exception.getErrorCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        var fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst();
        if (fieldError.isPresent()
                && "diaryRatingRequest".equals(fieldError.get().getObjectName())
                && "score".equals(fieldError.get().getField())) {
            return ApiResponse.fail(ErrorCode.DIARY_008);
        }
        if (fieldError.isPresent()
                && "commentCreateRequest".equals(fieldError.get().getObjectName())
                && "contentText".equals(fieldError.get().getField())) {
            return ApiResponse.fail(ErrorCode.COMMENT_003);
        }
        String message = fieldError
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse(ErrorCode.COMMON_001.getMessage());
        return ApiResponse.fail(ErrorCode.COMMON_001, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        return ApiResponse.fail(ErrorCode.COMMON_002, ErrorCode.COMMON_002.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception exception) {
        LOGGER.error("Unhandled backend exception", exception);
        return ApiResponse.fail(ErrorCode.COMMON_006);
    }

    private HttpStatus statusOf(ErrorCode errorCode) {
        return switch (errorCode) {
            case AUTH_002, AUTH_003, AUTH_004 -> HttpStatus.UNAUTHORIZED;
            case AUTH_005, AUTH_006, COMMENT_004, DIARY_011 -> HttpStatus.FORBIDDEN;
            case AUTH_001 -> HttpStatus.CONFLICT;
            case AUTH_009, COMMON_003, ROUTE_001, ROUTE_002, COMMENT_002, DIARY_003, AI_010 ->
                HttpStatus.NOT_FOUND;
            case AUTH_010, ROUTE_003, FILE_002, FILE_003, FILE_005,
                    IMPORT_002, IMPORT_003, IMPORT_004, COMMENT_001, COMMENT_005, DIARY_006,
                    AI_002, AI_007, AI_009 ->
                HttpStatus.UNPROCESSABLE_ENTITY;
            case ROUTE_010 -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
