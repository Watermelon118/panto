package com.panto.wms.user.service;

import com.panto.wms.auth.entity.User;
import com.panto.wms.auth.repository.UserRepository;
import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import com.panto.wms.user.dto.CreateUserRequest;
import com.panto.wms.user.dto.ResetUserPasswordRequest;
import com.panto.wms.user.dto.UpdateUserRequest;
import com.panto.wms.user.dto.UserPageResponse;
import com.panto.wms.user.dto.UserResponse;
import com.panto.wms.user.dto.UserSummaryResponse;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户管理业务服务。
 */
@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建用户管理服务。
     *
     * @param userRepository 用户数据访问接口
     * @param passwordEncoder 密码编码器
     */
    public UserManagementService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 返回分页用户列表。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页用户列表
     */
    @Transactional(readOnly = true)
    public UserPageResponse listUsers(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<User> users = userRepository.findAll(pageRequest);

        List<UserSummaryResponse> items = users.getContent().stream()
            .map(this::toSummaryResponse)
            .toList();

        return new UserPageResponse(
            items,
            users.getNumber(),
            users.getSize(),
            users.getTotalElements(),
            users.getTotalPages()
        );
    }

    /**
     * 返回用户详情。
     *
     * @param userId 用户 ID
     * @return 用户详情
     */
    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return toResponse(findUserOrThrow(userId));
    }

    /**
     * 创建用户。
     *
     * @param request 创建请求
     * @param operatorId 当前操作人 ID
     * @return 创建后的用户
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request, Long operatorId) {
        String normalizedUsername = normalizeRequired(request.username(), "username");
        validateUsernameUniqueness(normalizedUsername, null);

        OffsetDateTime now = OffsetDateTime.now();
        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(normalizeRequired(request.fullName(), "fullName"));
        user.setEmail(normalize(request.email()));
        user.setRole(request.role());
        user.setActive(true);
        user.setMustChangePassword(true);
        user.setLockedUntil(null);
        user.setLastLoginAt(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setCreatedBy(operatorId);
        user.setUpdatedBy(operatorId);

        return toResponse(userRepository.save(user));
    }

    /**
     * 更新用户基础信息。
     *
     * @param userId 用户 ID
     * @param request 更新请求
     * @param operatorId 当前操作人 ID
     * @return 更新后的用户
     */
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request, Long operatorId) {
        User user = findUserOrThrow(userId);
        user.setFullName(normalizeRequired(request.fullName(), "fullName"));
        user.setEmail(normalize(request.email()));
        user.setRole(request.role());
        user.setUpdatedAt(OffsetDateTime.now());
        user.setUpdatedBy(operatorId);

        return toResponse(userRepository.save(user));
    }

    /**
     * 更新用户启用状态。
     *
     * @param userId 用户 ID
     * @param active 目标启用状态
     * @param operatorId 当前操作人 ID
     * @return 更新后的用户
     */
    @Transactional
    public UserResponse updateUserStatus(Long userId, boolean active, Long operatorId) {
        User user = findUserOrThrow(userId);
        user.setActive(active);
        if (!active) {
            user.setLockedUntil(null);
        }
        user.setUpdatedAt(OffsetDateTime.now());
        user.setUpdatedBy(operatorId);

        return toResponse(userRepository.save(user));
    }

    /**
     * 重置用户密码。
     *
     * @param userId 用户 ID
     * @param request 重置密码请求
     * @param operatorId 当前操作人 ID
     * @return 更新后的用户
     */
    @Transactional
    public UserResponse resetPassword(Long userId, ResetUserPasswordRequest request, Long operatorId) {
        User user = findUserOrThrow(userId);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(true);
        user.setLockedUntil(null);
        user.setUpdatedAt(OffsetDateTime.now());
        user.setUpdatedBy(operatorId);

        return toResponse(userRepository.save(user));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateUsernameUniqueness(String username, Long userId) {
        boolean exists = userId == null
            ? userRepository.existsByUsernameIgnoreCase(username)
            : userRepository.existsByUsernameIgnoreCaseAndIdNot(username, userId);

        if (exists) {
            throw new BusinessException(ErrorCode.USER_USERNAME_ALREADY_EXISTS);
        }
    }

    private UserSummaryResponse toSummaryResponse(User user) {
        return new UserSummaryResponse(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getRole(),
            user.getActive(),
            user.getMustChangePassword()
        );
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getRole(),
            user.getActive(),
            user.getMustChangePassword(),
            user.getLockedUntil(),
            user.getLastLoginAt(),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            user.getCreatedBy(),
            user.getUpdatedBy()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + " must not be blank");
        }
        return normalized;
    }
}
