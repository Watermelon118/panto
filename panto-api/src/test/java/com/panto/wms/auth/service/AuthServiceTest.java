package com.panto.wms.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.panto.wms.auth.domain.LoginFailureReason;
import com.panto.wms.auth.domain.UserRole;
import com.panto.wms.auth.dto.ChangePasswordRequest;
import com.panto.wms.auth.dto.LoginRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 认证服务测试。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<LoginAttempt> loginAttemptCaptor;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void loginShouldReturnTokensWhenCredentialsAreValid() {
        User user = buildUser();
        when(jwtProperties.getAccessTokenTtl()).thenReturn(Duration.ofHours(2));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin", user.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(AuthenticatedUser.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(AuthenticatedUser.class))).thenReturn("refresh-token");

        AuthService.LoginResult result = authService.login(new LoginRequest("admin", "admin"), "127.0.0.1");

        assertEquals("access-token", result.response().accessToken());
        assertEquals("refresh-token", result.refreshToken());
        assertEquals("Bearer", result.response().tokenType());
        assertEquals(7200L, result.response().expiresIn());
        assertEquals(1L, result.response().userId());
        assertEquals("admin", result.response().username());
        assertEquals(UserRole.ADMIN, result.response().role());
        assertTrue(result.response().mustChangePassword());

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertNull(savedUser.getLockedUntil());
        assertNotNull(savedUser.getLastLoginAt());

        verify(loginAttemptRepository).save(loginAttemptCaptor.capture());
        LoginAttempt loginAttempt = loginAttemptCaptor.getValue();
        assertEquals("admin", loginAttempt.getUsername());
        assertEquals("127.0.0.1", loginAttempt.getIpAddress());
        assertTrue(loginAttempt.getSuccess());
        assertNull(loginAttempt.getFailureReason());
    }

    @Test
    void loginShouldRecordFailureWhenUserDoesNotExist() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.login(new LoginRequest("ghost", "bad-password"), "127.0.0.1")
        );

        assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS, exception.getErrorCode());
        verify(loginAttemptRepository).save(loginAttemptCaptor.capture());
        LoginAttempt loginAttempt = loginAttemptCaptor.getValue();
        assertEquals("ghost", loginAttempt.getUsername());
        assertEquals("127.0.0.1", loginAttempt.getIpAddress());
        assertEquals(Boolean.FALSE, loginAttempt.getSuccess());
        assertEquals(LoginFailureReason.USER_NOT_FOUND.name(), loginAttempt.getFailureReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginShouldLockUserAfterFiveConsecutiveFailures() {
        User user = buildUser();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);
        when(loginAttemptRepository.findAll(any(Example.class), any(Sort.class))).thenReturn(List.of(
            failedAttempt("admin"),
            failedAttempt("admin"),
            failedAttempt("admin"),
            failedAttempt("admin"),
            failedAttempt("admin")
        ));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.login(new LoginRequest("admin", "wrong-password"), "127.0.0.1")
        );

        assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS, exception.getErrorCode());
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertNotNull(savedUser.getLockedUntil());

        verify(loginAttemptRepository).save(loginAttemptCaptor.capture());
        LoginAttempt loginAttempt = loginAttemptCaptor.getValue();
        assertEquals(LoginFailureReason.BAD_CREDENTIALS.name(), loginAttempt.getFailureReason());
    }

    @Test
    void refreshShouldReturnNewTokensWhenRefreshTokenIsValid() {
        User user = buildUser();
        when(jwtProperties.getAccessTokenTtl()).thenReturn(Duration.ofHours(2));
        when(jwtTokenProvider.isRefreshTokenValid("refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getAuthenticatedUserFromRefreshToken("refresh-token"))
            .thenReturn(AuthenticatedUser.from(user));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(any(AuthenticatedUser.class))).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(any(AuthenticatedUser.class))).thenReturn("new-refresh-token");

        AuthService.LoginResult result = authService.refresh("refresh-token");

        assertEquals("new-access-token", result.response().accessToken());
        assertEquals("new-refresh-token", result.refreshToken());
        assertEquals(1L, result.response().userId());
        assertEquals(UserRole.ADMIN, result.response().role());
    }

    @Test
    void refreshShouldThrowUnauthorizedWhenRefreshTokenIsInvalid() {
        when(jwtTokenProvider.isRefreshTokenValid("bad-refresh-token")).thenReturn(false);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.refresh("bad-refresh-token")
        );

        assertEquals(ErrorCode.AUTH_UNAUTHORIZED, exception.getErrorCode());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void refreshShouldRejectDisabledUser() {
        User disabledUser = buildUser();
        disabledUser.setActive(false);

        when(jwtTokenProvider.isRefreshTokenValid(anyString())).thenReturn(true);
        when(jwtTokenProvider.getAuthenticatedUserFromRefreshToken(anyString()))
            .thenReturn(AuthenticatedUser.from(disabledUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(disabledUser));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.refresh("refresh-token")
        );

        assertEquals(ErrorCode.AUTH_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void changePasswordShouldUpdatePasswordAndClearMustChangeFlag() {
        User user = buildUser();
        when(jwtProperties.getAccessTokenTtl()).thenReturn(Duration.ofHours(2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("admin123", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("encoded-new-password");
        when(jwtTokenProvider.generateAccessToken(any(AuthenticatedUser.class))).thenReturn("changed-access-token");
        when(jwtTokenProvider.generateRefreshToken(any(AuthenticatedUser.class))).thenReturn("changed-refresh-token");

        AuthService.LoginResult result = authService.changePassword(
            1L,
            new ChangePasswordRequest("admin123", "newpass123")
        );

        assertEquals("changed-access-token", result.response().accessToken());
        assertEquals("changed-refresh-token", result.refreshToken());
        assertEquals(false, result.response().mustChangePassword());

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("encoded-new-password", savedUser.getPasswordHash());
        assertEquals(Boolean.FALSE, savedUser.getMustChangePassword());
    }

    @Test
    void changePasswordShouldRejectWrongCurrentPassword() {
        User user = buildUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.changePassword(1L, new ChangePasswordRequest("wrong", "newpass123"))
        );

        assertEquals(ErrorCode.AUTH_CURRENT_PASSWORD_INCORRECT, exception.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPasswordHash("$2a$12$test-password-hash");
        user.setFullName("System Administrator");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        user.setMustChangePassword(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        return user;
    }

    private LoginAttempt failedAttempt(String username) {
        LoginAttempt loginAttempt = new LoginAttempt();
        loginAttempt.setUsername(username);
        loginAttempt.setSuccess(false);
        return loginAttempt;
    }
}
