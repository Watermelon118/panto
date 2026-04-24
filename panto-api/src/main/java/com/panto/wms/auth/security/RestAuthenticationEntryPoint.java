package com.panto.wms.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panto.wms.common.api.Result;
import com.panto.wms.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 统一处理未认证请求，返回标准 JSON 响应。
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * 创建未认证处理器。
     *
     * @param objectMapper JSON 序列化工具
     */
    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Result<Void> result = Result.failure(
            ErrorCode.AUTH_UNAUTHORIZED.getCode(),
            ErrorCode.AUTH_UNAUTHORIZED.getDefaultMessage()
        );

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
