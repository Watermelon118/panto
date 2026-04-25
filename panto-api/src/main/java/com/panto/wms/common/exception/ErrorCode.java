package com.panto.wms.common.exception;

/**
 * 系统统一错误码定义。
 */
public enum ErrorCode {

    SUCCESS("SUCCESS", "成功"),
    VALIDATION_ERROR("VALIDATION_ERROR", "请求参数校验失败"),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "用户名或密码错误"),
    AUTH_ACCOUNT_LOCKED("AUTH_ACCOUNT_LOCKED", "账号已被临时锁定"),
    AUTH_UNAUTHORIZED("AUTH_UNAUTHORIZED", "未登录或令牌无效"),
    AUTH_FORBIDDEN("AUTH_FORBIDDEN", "没有权限访问该资源"),
    USER_NOT_FOUND("USER_NOT_FOUND", "用户不存在"),
    USER_USERNAME_ALREADY_EXISTS("USER_USERNAME_ALREADY_EXISTS", "用户名已存在"),
    CUSTOMER_NOT_FOUND("CUSTOMER_NOT_FOUND", "客户不存在"),
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "商品不存在"),
    PRODUCT_SKU_ALREADY_EXISTS("PRODUCT_SKU_ALREADY_EXISTS", "商品 SKU 已存在"),
    INBOUND_NOT_FOUND("INBOUND_NOT_FOUND", "入库单不存在"),
    INBOUND_PRODUCT_NOT_FOUND("INBOUND_PRODUCT_NOT_FOUND", "入库明细中包含不存在的商品"),
    INBOUND_HAS_STOCK_MOVEMENT("INBOUND_HAS_STOCK_MOVEMENT", "该入库单已有库存被使用，无法修改"),
    ORDER_CUSTOMER_NOT_FOUND("ORDER_CUSTOMER_NOT_FOUND", "客户不存在或已停用"),
    ORDER_PRODUCT_NOT_FOUND("ORDER_PRODUCT_NOT_FOUND", "订单中包含不存在的商品"),
    ORDER_PRODUCT_INACTIVE("ORDER_PRODUCT_INACTIVE", "订单中包含已停用的商品"),
    ORDER_DUPLICATE_PRODUCT("ORDER_DUPLICATE_PRODUCT", "订单中存在重复商品，请合并后再提交"),
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "订单不存在"),
    ORDER_ALREADY_ROLLED_BACK("ORDER_ALREADY_ROLLED_BACK", "订单已回滚，不能重复操作"),
    ORDER_INSUFFICIENT_STOCK("ORDER_INSUFFICIENT_STOCK", "库存不足，无法创建订单"),
    ORDER_STOCK_CONFLICT("ORDER_STOCK_CONFLICT", "库存已被其他操作更新，请重试"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "系统内部错误");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 返回提供给前端识别的错误码。
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
