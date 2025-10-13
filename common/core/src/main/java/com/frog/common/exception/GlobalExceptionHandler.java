package com.frog.common.exception;

import com.frog.common.response.ApiResponse;
import com.frog.common.response.ResultCode;

import javax.naming.AuthenticationException;
import java.nio.file.AccessDeniedException;

/**
 * 全局异常处理
 *
 * @author Deng
 * createData 2025/10/11 14:33
 * @version 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ApiResponse<Void> handleAuthenticationException(AuthenticationException ex) {
        return ApiResponse.fail(ResultCode.UNAUTHORIZED.getCode(), "Unauthorized: " + ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException ex) {
        return ApiResponse.fail(ResultCode.FORBIDDEN.getCode(), "Forbidden: " + ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationError(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ApiResponse.fail(ResultCode.VALIDATION_FAILED.getCode(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException ex) {
        return ApiResponse.fail(ResultCode.VALIDATION_FAILED.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleGenericException(Exception ex) {
        return ApiResponse.fail(ResultCode.SERVER_ERROR.getCode(), ex.getMessage());
    }
}
