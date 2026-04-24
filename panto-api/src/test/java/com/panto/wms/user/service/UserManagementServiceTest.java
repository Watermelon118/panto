package com.panto.wms.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.panto.wms.auth.domain.UserRole;
import com.panto.wms.auth.entity.User;
import com.panto.wms.auth.repository.UserRepository;
import com.panto.wms.common.exception.BusinessException;
import com.panto.wms.common.exception.ErrorCode;
import com.panto.wms.user.dto.CreateUserRequest;
import com.panto.wms.user.dto.ResetUserPasswordRequest;
import com.panto.wms.user.dto.UpdateUserRequest;
import com.panto.wms.user.dto.UserPageResponse;
import com.panto.wms.user.dto.UserResponse;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 用户管理服务测试。
 */
@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserManagementService userManagementService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void createUserShouldSaveUserWhenRequestIsValid() {
        CreateUserRequest request = new CreateUserRequest(
            "john.doe",
            "Password1",
            "John Doe",
            "john@panto.co.nz",
            UserRole.WAREHOUSE
        );
        User savedUser = buildUser(1L, "john.doe", UserRole.WAREHOUSE);

        when(userRepository.existsByUsernameIgnoreCase("john.doe")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userManagementService.createUser(request, 1L);

        assertEquals(1L, response.id());
        assertEquals("john.doe", response.username());
        assertEquals("John Doe", response.fullName());
        assertTrue(response.active());
        assertTrue(response.mustChangePassword());

        verify(userRepository).save(userCaptor.capture());
        User captured = userCaptor.getValue();
        assertEquals("john.doe", captured.getUsername());
        assertEquals("hashed", captured.getPasswordHash());
        assertEquals("John Doe", captured.getFullName());
        assertEquals("john@panto.co.nz", captured.getEmail());
        assertEquals(UserRole.WAREHOUSE, captured.getRole());
        assertTrue(captured.getActive());
        assertTrue(captured.getMustChangePassword());
        assertNull(captured.getLockedUntil());
        assertNull(captured.getLastLoginAt());
        assertEquals(1L, captured.getCreatedBy());
        assertEquals(1L, captured.getUpdatedBy());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
    }

    @Test
    void createUserShouldThrowWhenUsernameAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest(
            "john.doe", "Password1", "John Doe", null, UserRole.WAREHOUSE
        );

        when(userRepository.existsByUsernameIgnoreCase("john.doe")).thenReturn(true);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userManagementService.createUser(request, 1L)
        );

        assertEquals(ErrorCode.USER_USERNAME_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void updateUserShouldSaveUpdatedFieldsWhenUserExists() {
        User existingUser = buildUser(2L, "jane.doe", UserRole.MARKETING);
        UpdateUserRequest request = new UpdateUserRequest(
            "Jane Smith", "jane.smith@panto.co.nz", UserRole.ACCOUNTANT
        );

        when(userRepository.findById(2L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userManagementService.updateUser(2L, request, 1L);

        assertEquals("Jane Smith", response.fullName());
        assertEquals("jane.smith@panto.co.nz", response.email());
        assertEquals(UserRole.ACCOUNTANT, response.role());

        verify(userRepository).save(userCaptor.capture());
        User captured = userCaptor.getValue();
        assertEquals("Jane Smith", captured.getFullName());
        assertEquals("jane.smith@panto.co.nz", captured.getEmail());
        assertEquals(UserRole.ACCOUNTANT, captured.getRole());
        assertEquals(1L, captured.getUpdatedBy());
        assertNotNull(captured.getUpdatedAt());
    }

    @Test
    void getUserShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userManagementService.getUser(99L)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateUserStatusShouldPersistActiveFlagWhenDeactivating() {
        User existingUser = buildUser(3L, "bob", UserRole.WAREHOUSE);
        existingUser.setLockedUntil(OffsetDateTime.now().plusMinutes(5));

        when(userRepository.findById(3L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userManagementService.updateUserStatus(3L, false, 1L);

        assertFalse(response.active());

        verify(userRepository).save(userCaptor.capture());
        User captured = userCaptor.getValue();
        assertFalse(captured.getActive());
        assertNull(captured.getLockedUntil());
        assertEquals(1L, captured.getUpdatedBy());
        assertNotNull(captured.getUpdatedAt());
    }

    @Test
    void updateUserStatusShouldNotClearLockedUntilWhenActivating() {
        User existingUser = buildUser(4L, "carol", UserRole.WAREHOUSE);
        existingUser.setActive(false);
        OffsetDateTime lockedUntil = OffsetDateTime.now().plusMinutes(5);
        existingUser.setLockedUntil(lockedUntil);

        when(userRepository.findById(4L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userManagementService.updateUserStatus(4L, true, 1L);

        verify(userRepository).save(userCaptor.capture());
        User captured = userCaptor.getValue();
        assertTrue(captured.getActive());
        assertEquals(lockedUntil, captured.getLockedUntil());
    }

    @Test
    void resetPasswordShouldEncodeNewPasswordAndSetMustChangePassword() {
        User existingUser = buildUser(5L, "dave", UserRole.WAREHOUSE);
        existingUser.setMustChangePassword(false);
        existingUser.setLockedUntil(OffsetDateTime.now().plusMinutes(5));
        ResetUserPasswordRequest request = new ResetUserPasswordRequest("NewPass1");

        when(userRepository.findById(5L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("NewPass1")).thenReturn("new-hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userManagementService.resetPassword(5L, request, 1L);

        assertTrue(response.mustChangePassword());

        verify(userRepository).save(userCaptor.capture());
        User captured = userCaptor.getValue();
        assertEquals("new-hashed", captured.getPasswordHash());
        assertTrue(captured.getMustChangePassword());
        assertNull(captured.getLockedUntil());
        assertEquals(1L, captured.getUpdatedBy());
        assertNotNull(captured.getUpdatedAt());
    }

    @Test
    void listUsersShouldReturnPagedItems() {
        User user = buildUser(1L, "john.doe", UserRole.WAREHOUSE);
        PageImpl<User> page = new PageImpl<>(
            List.of(user),
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt")),
            1
        );

        when(userRepository.findAll(
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt"))
        )).thenReturn(page);

        UserPageResponse response = userManagementService.listUsers(0, 20);

        assertEquals(1, response.items().size());
        assertEquals("john.doe", response.items().getFirst().username());
        assertEquals(1L, response.totalElements());
        assertEquals(1, response.totalPages());
    }

    private User buildUser(Long id, String username, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("hashed");
        user.setFullName("John Doe");
        user.setEmail("john@panto.co.nz");
        user.setRole(role);
        user.setActive(true);
        user.setMustChangePassword(true);
        user.setLockedUntil(null);
        user.setLastLoginAt(null);
        user.setCreatedAt(OffsetDateTime.now().minusDays(1));
        user.setUpdatedAt(OffsetDateTime.now().minusHours(1));
        user.setCreatedBy(1L);
        user.setUpdatedBy(1L);
        return user;
    }
}
