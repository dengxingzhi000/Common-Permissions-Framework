package com.frog.common.response;

import lombok.Getter;

/**
 * 统一状态码枚举
 *
 * @author Deng
 * createData 2025/10/11 14:31
 * @version 1.0
 */
@Getter
public enum ResultCode {
    SUCCESS(200, "Success"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    VALIDATION_FAILED(422, "Validation Failed"),
    SERVER_ERROR(500, "Internal Server Error");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}