package com.panto.wms.auth.service;

import com.panto.wms.auth.domain.LoginFailureReason;
import com.panto.wms.auth.dto.LoginRequest;
import com.panto.wms.auth.dto.LoginResponse;
import com.panto.wms.auth.entity.LoginAttempt;
import com.panto.wms.auth.entity.User;
import com.panto.wms.auth.repository.LoginAttemptRepository;
import com.panto.wms.auth.repository.UserRepository;
import com.panto.wms.auth.security.AuthenticatedUser;
import com.panto.wms.auth.security.JwtProperties;
import com.panto.wms.auth.security.JwtTokenProvider;
import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Example;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证相关业务服务。
 */
@Service
public class AuthService {

    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private static final Duration ACCOUNT_LOCK_DURATION = Duration.ofMinutes(10);

    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    /**
     * 创建认证服务。
     *
     * @param userRepository 用户数据访问接口
     * @param loginAttemptRepository 登录尝试数据访问接口
     * @param passwordEncoder 密码编码器
     * @param jwtTokenProvider JWT 处理组件
     * @param jwtProperties JWT 配置
     */
    public AuthService(
        UserRepository userRepository,
        LoginAttemptRepository loginAttemptRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider,
        JwtProperties jwtProperties
    ) {
        this.userRepository = userRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 执行登录并返回登录结果。
     *
     * @param request 登录请求
     * @param ipAddress 客户端 IP
     * @return 登录结果
     */
    @Transactional
    public LoginResult login(LoginRequest request, String ipAddress) {
        String username = request.username().trim();
        OffsetDateTime now = OffsetDateTime.now();

        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            recordFailedAttempt(username, ipAddress, LoginFailureReason.USER_NOT_FOUND, now);
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        User user = userOptional.get();

        if (Boolean.FALSE.equals(user.getActive())) {
            recordFailedAttempt(username, ipAddress, LoginFailureReason.ACCOUNT_DISABLED, now);
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN, "账号已停用");
        }

        if (isUserLocked(user, now)) {
            recordFailedAttempt(username, ipAddress, LoginFailureReason.ACCOUNT_LOCKED, now);
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED, "账号已被锁定，请10分钟后再试");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordFailedAttempt(username, ipAddress, LoginFailureReason.BAD_CREDENTIALS, now);
            lockUserIfNeeded(user, now);
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        recordSuccessAttempt(username, ipAddress, now);

        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);
        String accessToken = jwtTokenProvider.generateAccessToken(authenticatedUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authenticatedUser);

        LoginResponse response = new LoginResponse(
            accessToken,
            "Bearer",
            jwtProperties.getAccessTokenTtl().toSeconds(),
            user.getId(),
            user.getUsername(),
            user.getRole(),
            Boolean.TRUE.equals(user.getMustChangePassword())
        );

        return new LoginResult(response, refreshToken);
    }

    private boolean isUserLocked(User user, OffsetDateTime now) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(now);
    }

    private void lockUserIfNeeded(User user, OffsetDateTime now) {
        List<LoginAttempt> recentAttempts = findRecentAttempts(user.getUsername());

        boolean shouldLock = recentAttempts.size() >= MAX_CONSECUTIVE_FAILURES
            && recentAttempts.stream()
                .limit(MAX_CONSECUTIVE_FAILURES)
                .allMatch(attempt -> Boolean.FALSE.equals(attempt.getSuccess()));

        if (shouldLock) {
            user.setLockedUntil(now.plus(ACCOUNT_LOCK_DURATION));
            user.setUpdatedAt(now);
            userRepository.save(user);
        }
    }

    private List<LoginAttempt> findRecentAttempts(String username) {
        LoginAttempt probe = new LoginAttempt();
        probe.setUsername(username);

        return loginAttemptRepository.findAll(
            Example.of(probe),
            Sort.by(Direction.DESC, "createdAt")
        );
    }

    private void recordSuccessAttempt(String username, String ipAddress, OffsetDateTime now) {
        LoginAttempt loginAttempt = new LoginAttempt();
        loginAttempt.setUsername(username);
        loginAttempt.setIpAddress(ipAddress);
        loginAttempt.setSuccess(true);
        loginAttempt.setFailureReason(null);
        loginAttempt.setCreatedAt(now);
        loginAttemptRepository.save(loginAttempt);
    }

    private void recordFailedAttempt(
        String username,
        String ipAddress,
        LoginFailureReason failureReason,
        OffsetDateTime now
    ) {
        LoginAttempt loginAttempt = new LoginAttempt();
        loginAttempt.setUsername(username);
        loginAttempt.setIpAddress(ipAddress);
        loginAttempt.setSuccess(false);
        loginAttempt.setFailureReason(failureReason.name());
        loginAttempt.setCreatedAt(now);
        loginAttemptRepository.save(loginAttempt);
    }

    /**
     * 登录成功后的内部结果。
     *
     * @param response 响应体数据
     * @param refreshToken Refresh Token
     */
    public record LoginResult(LoginResponse response, String refreshToken) {
    }
}
