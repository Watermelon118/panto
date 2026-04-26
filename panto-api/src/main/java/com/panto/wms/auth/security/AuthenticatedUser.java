package com.panto.wms.auth.security;

import com.panto.wms.auth.domain.UserRole;
import com.panto.wms.auth.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security 运行时使用的当前登录用户信息。
 */
public class AuthenticatedUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final UserRole role;
    private final boolean active;
    private final boolean mustChangePassword;

    /**
     * 创建认证用户对象。
     *
     * @param userId 用户 ID
     * @param username 用户名
     * @param role 用户角色
     * @param active 用户是否启用
     * @param mustChangePassword 是否必须修改密码
     */
    public AuthenticatedUser(
        Long userId,
        String username,
        UserRole role,
        boolean active,
        boolean mustChangePassword
    ) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.active = active;
        this.mustChangePassword = mustChangePassword;
    }

    /**
     * 根据用户实体创建认证用户对象。
     *
     * @param user 用户实体
     * @return 认证用户对象
     */
    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            Boolean.TRUE.equals(user.getActive()),
            Boolean.TRUE.equals(user.getMustChangePassword())
        );
    }

    /**
     * 返回当前用户主键。
     *
     * @return 用户主键
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 返回当前用户角色。
     *
     * @return 用户角色
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * 返回当前用户是否必须先修改密码。
     *
     * @return 是否必须修改密码
     */
    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.asAuthority()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
