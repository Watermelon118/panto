package com.panto.wms.auth.controller;

import com.panto.wms.auth.dto.LoginRequest;
import com.panto.wms.auth.dto.LoginResponse;
import com.panto.wms.auth.security.JwtProperties;
import com.panto.wms.auth.service.AuthService;
import com.panto.wms.common.api.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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

        ResponseCookie refreshCookie = ResponseCookie.from(
                jwtProperties.getRefreshCookieName(),
                loginResult.refreshToken()
            )
            .httpOnly(true)
            .secure(false)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(jwtProperties.getRefreshTokenTtl())
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(Result.success(loginResult.response()));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
