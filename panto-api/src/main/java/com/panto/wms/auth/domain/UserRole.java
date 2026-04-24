package com.panto.wms.auth.domain;

/**
 * 系统用户角色定义。
 */
public enum UserRole {

    ADMIN,
    WAREHOUSE,
    MARKETING,
    ACCOUNTANT;

    /**
     * 转换为 Spring Security 常用的角色标识。
     *
     * @return 角色标识
     */
    public String asAuthority() {
        return "ROLE_" + name();
    }
}
