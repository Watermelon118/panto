package com.panto.wms.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.panto.wms.auth.domain.UserRole;
import com.panto.wms.auth.dto.LoginResponse;
import com.panto.wms.auth.security.JwtProperties;
import com.panto.wms.auth.service.AuthService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 认证控制器测试。
 */
class AuthControllerTest {

    @Test
    void loginShouldSetSecureRefreshCookieWhenConfigured() throws Exception {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setRefreshCookieName("refresh_token");
        jwtProperties.setRefreshTokenTtl(Duration.ofDays(7));
        jwtProperties.setRefreshCookieSecure(true);

        AuthService authService = org.mockito.Mockito.mock(AuthService.class);
        LoginResponse response = new LoginResponse(
            "access-token",
            "Bearer",
            7200,
            1L,
            "admin",
            UserRole.ADMIN,
            false
        );
        when(authService.login(any(), eq("127.0.0.1"))).thenReturn(
            new AuthService.LoginResult(response, "refresh-token")
        );

        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AuthController(authService, jwtProperties))
            .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "admin",
                      "password": "password"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")));
    }
}
