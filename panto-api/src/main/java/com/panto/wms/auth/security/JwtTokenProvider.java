package com.panto.wms.auth.security;

import com.panto.wms.auth.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * 负责生成、解析和校验 JWT。
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_MUST_CHANGE_PASSWORD = "must_change_password";
    private static final String CLAIM_TOKEN_TYPE = "typ";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    /**
     * 创建 JWT 处理组件。
     *
     * @param jwtProperties JWT 配置
     */
    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 生成 Access Token。
     *
     * @param user 当前认证用户
     * @return Access Token
     */
    public String generateAccessToken(AuthenticatedUser user) {
        return buildToken(user, TOKEN_TYPE_ACCESS);
    }

    /**
     * 生成 Refresh Token。
     *
     * @param user 当前认证用户
     * @return Refresh Token
     */
    public String generateRefreshToken(AuthenticatedUser user) {
        return buildToken(user, TOKEN_TYPE_REFRESH);
    }

    /**
     * 校验 Access Token 是否有效。
     *
     * @param token JWT 字符串
     * @return 是否有效
     */
    public boolean isAccessTokenValid(String token) {
        return isTokenValid(token, TOKEN_TYPE_ACCESS);
    }

    /**
     * 校验 Refresh Token 是否有效。
     *
     * @param token JWT 字符串
     * @return 是否有效
     */
    public boolean isRefreshTokenValid(String token) {
        return isTokenValid(token, TOKEN_TYPE_REFRESH);
    }

    /**
     * 从 Access Token 还原当前认证用户。
     *
     * @param token Access Token
     * @return 认证用户
     */
    public AuthenticatedUser getAuthenticatedUserFromAccessToken(String token) {
        Claims claims = parseClaims(token, TOKEN_TYPE_ACCESS);

        Long userId = claims.get(CLAIM_USER_ID, Long.class);
        String username = claims.getSubject();
        UserRole role = UserRole.valueOf(claims.get(CLAIM_ROLE, String.class));
        Boolean mustChangePassword = claims.get(CLAIM_MUST_CHANGE_PASSWORD, Boolean.class);

        return new AuthenticatedUser(userId, username, role, true, Boolean.TRUE.equals(mustChangePassword));
    }

    /**
     * 从 Refresh Token 读取用户主键。
     *
     * @param token Refresh Token
     * @return 用户主键
     */
    public Long getUserIdFromRefreshToken(String token) {
        return parseClaims(token, TOKEN_TYPE_REFRESH).get(CLAIM_USER_ID, Long.class);
    }

    /**
     * 从 Refresh Token 还原当前认证用户。
     *
     * @param token Refresh Token
     * @return 认证用户
     */
    public AuthenticatedUser getAuthenticatedUserFromRefreshToken(String token) {
        Claims claims = parseClaims(token, TOKEN_TYPE_REFRESH);

        Long userId = claims.get(CLAIM_USER_ID, Long.class);
        String username = claims.getSubject();
        UserRole role = UserRole.valueOf(claims.get(CLAIM_ROLE, String.class));
        Boolean mustChangePassword = claims.get(CLAIM_MUST_CHANGE_PASSWORD, Boolean.class);

        return new AuthenticatedUser(userId, username, role, true, Boolean.TRUE.equals(mustChangePassword));
    }

    private String buildToken(AuthenticatedUser user, String tokenType) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(resolveTtl(tokenType));

        return Jwts.builder()
            .issuer(jwtProperties.getIssuer())
            .subject(user.getUsername())
            .claim(CLAIM_USER_ID, user.getUserId())
            .claim(CLAIM_ROLE, user.getRole().name())
            .claim(CLAIM_MUST_CHANGE_PASSWORD, user.isMustChangePassword())
            .claim(CLAIM_TOKEN_TYPE, tokenType)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(resolveSigningKey(tokenType))
            .compact();
    }

    private boolean isTokenValid(String token, String expectedTokenType) {
        try {
            parseClaims(token, expectedTokenType);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token, String expectedTokenType) {
        Claims claims = Jwts.parser()
            .verifyWith(resolveSigningKey(expectedTokenType))
            .build()
            .parseSignedClaims(token)
            .getPayload();

        if (!jwtProperties.getIssuer().equals(claims.getIssuer())) {
            throw new JwtException("JWT issuer 不匹配");
        }

        String actualTokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!expectedTokenType.equals(actualTokenType)) {
            throw new JwtException("JWT token 类型不匹配");
        }

        return claims;
    }

    private SecretKey resolveSigningKey(String tokenType) {
        String secret = TOKEN_TYPE_REFRESH.equals(tokenType)
            ? jwtProperties.getRefreshTokenSecret()
            : jwtProperties.getAccessTokenSecret();

        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private java.time.Duration resolveTtl(String tokenType) {
        return TOKEN_TYPE_REFRESH.equals(tokenType)
            ? jwtProperties.getRefreshTokenTtl()
            : jwtProperties.getAccessTokenTtl();
    }
}
