package com.panto.wms.common.exception;

/**
 * 系统统一错误码定义。
 */
public enum ErrorCode {

    SUCCESS("SUCCESS", "成功"),
    VALIDATION_ERROR("VALIDATION_ERROR", "请求参数校验失败"),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "用户名或密码错误"),
    AUTH_ACCOUNT_LOCKED("AUTH_ACCOUNT_LOCKED", "账号已被锁定"),
    AUTH_UNAUTHORIZED("AUTH_UNAUTHORIZED", "未登录或登录已失效"),
    AUTH_FORBIDDEN("AUTH_FORBIDDEN", "无权访问该资源"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "系统内部错误");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 返回供前端识别的错误码。
     *
     * @return 错误码
     */
    public String getCode() {
        return code;
    }

    /**
     * 返回默认错误消息。
     *
     * @return 默认错误消息
     */
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
