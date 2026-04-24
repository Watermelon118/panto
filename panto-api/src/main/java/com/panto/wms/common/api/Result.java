package com.panto.wms.common.api;

import java.util.Objects;

/**
 * 所有 REST 接口统一使用的响应外层结构。
 *
 * @param code 前端可识别的响应码
 * @param message 用于展示或排查问题的响应消息
 * @param data 实际返回数据
 * @param <T> 数据类型
 */
public record Result<T>(String code, String message, T data) {

    /**
     * 创建一个带数据的成功响应。
     *
     * @param data 返回数据
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>("SUCCESS", "Success", data);
    }

    /**
     * 创建一个不带数据的成功响应。
     *
     * @return 成功响应
     */
    public static Result<Void> success() {
        return new Result<>("SUCCESS", "Success", null);
    }

    /**
     * 创建一个失败响应。
     *
     * @param code 业务错误码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return 失败响应
     */
    public static <T> Result<T> failure(String code, String message) {
        return new Result<>(Objects.requireNonNull(code, "code must not be null"), message, null);
    }
}
