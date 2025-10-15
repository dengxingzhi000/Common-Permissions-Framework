package com.frog.exception;

/**
 * 业务异常类
 *
 * @author Deng
 * createData 2025/10/15 14:26
 * @version 1.0
 */
public class BusinessException extends RuntimeException {

    private Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
