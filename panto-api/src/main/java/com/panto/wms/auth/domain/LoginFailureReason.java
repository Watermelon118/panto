package com.panto.wms.auth.domain;

/**
 * 登录失败原因定义。
 */
public enum LoginFailureReason {

    USER_NOT_FOUND,
    BAD_CREDENTIALS,
    ACCOUNT_LOCKED,
    ACCOUNT_DISABLED
}
