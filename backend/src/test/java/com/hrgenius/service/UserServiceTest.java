package com.hrgenius.service;

import com.hrgenius.dto.common.PagedResponse;
import com.hrgenius.dto.user.UserResponse;
import com.hrgenius.entity.Employee;
import com.hrgenius.entity.Role;
import com.hrgenius.entity.User;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.repository.RefreshTokenRepository;
import com.hrgenius.repository.RoleRepository;
import com.hrgenius.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role employeeRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        employeeRole = new Role(Role.RoleName.EMPLOYEE);
        employeeRole.setId(1L);

        adminRole = new Role(Role.RoleName.ADMIN);
        adminRole.setId(2L);

        testUser = new User();
        testUser.setId(10L);
        testUser.setUsername("emp001");
        testUser.setEmail("emp001@example.com");
        testUser.setPassword("encodedPwd");
        testUser.setEnabled(true);
        testUser.setMustChangePassword(true);
        testUser.setFailedAttempts(3);
        testUser.setLockoutTime(LocalDateTime.now().plusHours(1));
        testUser.setRoles(new HashSet<>(Collections.singletonList(employeeRole)));

        Employee emp = new Employee();
        emp.setId(100L);
        emp.setEmployeeCode("EMP001");
        emp.setFirstName("Jane");
        emp.setLastName("Doe");
        testUser.setEmployee(emp);
    }

    @Test
    void testGetAllUsers() {
        Page<User> page = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PagedResponse<UserResponse> response = userService.getAllUsers("emp", true, 0, 10, "id", "asc");

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        UserResponse ur = response.getContent().get(0);
        assertEquals("emp001", ur.getUsername());
        assertEquals("emp001@example.com", ur.getEmail());
        assertTrue(ur.isLocked());
        assertEquals(100L, ur.getEmployeeId());
        assertEquals("Jane Doe", ur.getEmployeeName());
        assertTrue(ur.getRoles().contains("EMPLOYEE"));
    }

    @Test
    void testGetUserById_Success() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));

        UserResponse res = userService.getUserById(10L);

        assertNotNull(res);
        assertEquals("emp001", res.getUsername());
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(999L));
    }

    @Test
    void testUpdateUserStatus_Disable() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse res = userService.updateUserStatus(10L, false);

        assertFalse(res.isEnabled());
        verify(refreshTokenRepository).revokeAllByUser(testUser);
        verify(userRepository).save(testUser);
    }

    @Test
    void testUnlockUser() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse res = userService.unlockUser(10L);

        assertEquals(0, res.getFailedAttempts());
        assertNull(res.getLockoutTime());
        assertFalse(res.isLocked());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUserRoles_Success() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(Role.RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse res = userService.updateUserRoles(10L, Set.of("ADMIN"));

        assertTrue(res.getRoles().contains("ADMIN"));
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUserRoles_InvalidRole() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> userService.updateUserRoles(10L, Set.of("SUPER_ADMIN")));
    }
}
