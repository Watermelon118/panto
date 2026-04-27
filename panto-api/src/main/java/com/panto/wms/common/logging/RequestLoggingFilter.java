package com.panto.wms.common.logging;

import com.panto.wms.auth.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 记录每个 HTTP 请求的运行日志，便于云端排查慢请求和错误响应。
 */
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    /**
     * 请求追踪 ID 响应头名称。
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 日志 MDC 中保存请求追踪 ID 的键名。
     */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private static final String ANONYMOUS_USER = "anonymous";
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        long startTime = System.nanoTime();
        String traceId = resolveTraceId(request);

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            logRequest(request, response, durationMs);
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String incomingTraceId = request.getHeader(TRACE_ID_HEADER);
        if (StringUtils.hasText(incomingTraceId)) {
            return incomingTraceId;
        }
        return UUID.randomUUID().toString();
    }

    private void logRequest(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        int status = response.getStatus();
        String method = request.getMethod();
        String path = resolvePath(request);
        String user = resolveCurrentUser();
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        String clientIp = resolveClientIp(request);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);

        if (status >= 500) {
            log.error(
                "HTTP request completed: traceId={}, method={}, path={}, status={}, durationMs={}, user={}, clientIp={}, userAgent={}",
                traceId,
                method,
                path,
                status,
                durationMs,
                user,
                clientIp,
                userAgent
            );
        } else if (status >= 400) {
            log.warn(
                "HTTP request completed: traceId={}, method={}, path={}, status={}, durationMs={}, user={}, clientIp={}, userAgent={}",
                traceId,
                method,
                path,
                status,
                durationMs,
                user,
                clientIp,
                userAgent
            );
        } else {
            log.info(
                "HTTP request completed: traceId={}, method={}, path={}, status={}, durationMs={}, user={}, clientIp={}, userAgent={}",
                traceId,
                method,
                path,
                status,
                durationMs,
                user,
                clientIp,
                userAgent
            );
        }
    }

    private String resolvePath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ANONYMOUS_USER;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.getUserId() + ":" + authenticatedUser.getUsername();
        }

        if (principal instanceof String username && StringUtils.hasText(username)) {
            return username;
        }

        String authenticationName = authentication.getName();
        if (StringUtils.hasText(authenticationName)) {
            return authenticationName;
        }
        return ANONYMOUS_USER;
    }
}
