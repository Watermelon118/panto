package com.panto.wms.common.exception;

import java.util.Objects;

/**
 * 用于表示可预期的业务异常。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 使用错误码默认消息创建业务异常。
     *
     * @param errorCode 错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").getDefaultMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用自定义消息创建业务异常。
     *
     * @param errorCode 错误码
     * @param message 自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    /**
     * 返回业务异常对应的错误码。
     *
     * @return 错误码
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
