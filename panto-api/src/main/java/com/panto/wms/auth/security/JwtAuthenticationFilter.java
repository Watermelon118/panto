package com.panto.wms.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panto.wms.common.api.Result;
import com.panto.wms.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 从请求头中解析 JWT，并将认证信息写入 Spring Security 上下文。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    /**
     * 创建 JWT 认证过滤器。
     *
     * @param jwtTokenProvider JWT 处理组件
     * @param objectMapper JSON 序列化组件
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveBearerToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.isAccessTokenValid(token)) {
            AuthenticatedUser authenticatedUser = jwtTokenProvider.getAuthenticatedUserFromAccessToken(token);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                authenticatedUser.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            if (authenticatedUser.isMustChangePassword() && !isPasswordChangeAllowedRequest(request)) {
                writePasswordChangeRequiredResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    private boolean isPasswordChangeAllowedRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith(AUTH_PATH_PREFIX);
    }

    private void writePasswordChangeRequiredResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
            response.getWriter(),
            Result.failure(
                ErrorCode.AUTH_PASSWORD_CHANGE_REQUIRED.getCode(),
                ErrorCode.AUTH_PASSWORD_CHANGE_REQUIRED.getDefaultMessage()
            )
        );
    }
}
