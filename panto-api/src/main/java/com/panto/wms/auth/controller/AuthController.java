package com.panto.wms.auth.controller;

import com.panto.wms.auth.dto.ChangePasswordRequest;
import com.panto.wms.auth.dto.LoginRequest;
import com.panto.wms.auth.dto.LoginResponse;
import com.panto.wms.auth.security.JwtProperties;
import com.panto.wms.auth.security.AuthenticatedUser;
import com.panto.wms.auth.service.AuthService;
import com.panto.wms.common.api.Result;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证相关接口控制器。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    /**
     * 创建认证控制器。
     *
     * @param authService 认证服务
     * @param jwtProperties JWT 配置
     */
    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @param httpServletRequest HTTP 请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public ResponseEntity<Result<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpServletRequest
    ) {
        AuthService.LoginResult loginResult = authService.login(request, resolveClientIp(httpServletRequest));
        return buildTokenResponse(loginResult);
    }

    /**
     * 刷新 Access Token。
     *
     * @param httpServletRequest HTTP 请求
     * @return 刷新结果
     */
    @PostMapping("/refresh")
    public ResponseEntity<Result<LoginResponse>> refresh(HttpServletRequest httpServletRequest) {
        String refreshToken = extractRefreshToken(httpServletRequest);
        AuthService.LoginResult loginResult = authService.refresh(refreshToken);
        return buildTokenResponse(loginResult);
    }

    /**
     * 修改当前登录用户密码。
     *
     * @param request 修改密码请求
     * @param authenticatedUser 当前登录用户
     * @return 修改后的登录结果
     */
    @PostMapping("/change-password")
    public ResponseEntity<Result<LoginResponse>> changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        AuthService.LoginResult loginResult = authService.changePassword(authenticatedUser.getUserId(), request);
        return buildTokenResponse(loginResult);
    }

    /**
     * 退出登录并清理 Refresh Cookie。
     *
     * @return 成功响应
     */
    @PostMapping("/logout")
    public ResponseEntity<Result<Void>> logout() {
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, buildRefreshCookie("", 0).toString())
            .body(Result.success());
    }

    private ResponseEntity<Result<LoginResponse>> buildTokenResponse(AuthService.LoginResult loginResult) {
        return ResponseEntity.ok()
            .header(
                HttpHeaders.SET_COOKIE,
                buildRefreshCookie(loginResult.refreshToken(), jwtProperties.getRefreshTokenTtl().toSeconds()).toString()
            )
            .body(Result.success(loginResult.response()));
    }

    private ResponseCookie buildRefreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), value)
            .httpOnly(true)
            .secure(jwtProperties.isRefreshCookieSecure())
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(maxAgeSeconds)
            .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (jwtProperties.getRefreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
