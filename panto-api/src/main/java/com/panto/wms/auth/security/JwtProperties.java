package com.panto.wms.auth.security;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 相关配置。
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "panto.jwt")
public class JwtProperties {

    private String issuer = "panto-api";
    private String accessTokenSecret = "replace-me-access-token-secret-for-local-dev-only";
    private String refreshTokenSecret = "replace-me-refresh-token-secret-for-local-dev-only";
    private Duration accessTokenTtl = Duration.ofHours(2);
    private Duration refreshTokenTtl = Duration.ofDays(7);
    private String refreshCookieName = "refresh_token";
    private boolean refreshCookieSecure = false;
}
