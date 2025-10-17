package com.frog.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 *
 * @author Deng
 * createData 2025/10/16 15:38
 * @version 1.0
 */
@Getter
public class ServiceException extends RuntimeException {
    private final Integer code;

    public ServiceException(String message) {
        super(message);
        this.code = 500;
    }

    public ServiceException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
}
