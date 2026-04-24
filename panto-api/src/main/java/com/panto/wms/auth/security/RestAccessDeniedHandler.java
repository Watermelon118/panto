package com.panto.wms.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panto.wms.common.api.Result;
import com.panto.wms.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 统一处理无权限访问请求，返回标准 JSON 响应。
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * 创建无权限处理器。
     *
     * @param objectMapper JSON 序列化工具
     */
    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Result<Void> result = Result.failure(
            ErrorCode.AUTH_FORBIDDEN.getCode(),
            ErrorCode.AUTH_FORBIDDEN.getDefaultMessage()
        );

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
