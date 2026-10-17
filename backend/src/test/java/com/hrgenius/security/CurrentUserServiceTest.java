package com.hrgenius.security;

import com.hrgenius.entity.Employee;
import com.hrgenius.entity.Role;
import com.hrgenius.entity.User;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.repository.EmployeeRepository;
import com.hrgenius.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private CurrentUserService currentUserService;

    private User testUser;
    private UserPrincipal testPrincipal;
    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        Role employeeRole = new Role();
        employeeRole.setId(1L);
        employeeRole.setName(Role.RoleName.EMPLOYEE);

        testUser = new User();
        testUser.setId(100L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@hrgenius.com");
        testUser.setRoles(Set.of(employeeRole));

        testPrincipal = new UserPrincipal(testUser);

        testEmployee = new Employee();
        testEmployee.setId(200L);
        testEmployee.setUser(testUser);
        testEmployee.setEmail("test@hrgenius.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetCurrentUserPrincipal_WhenAuthenticated() {
        Authentication auth = new UsernamePasswordAuthenticationToken(testPrincipal, null, testPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<UserPrincipal> principal = currentUserService.getCurrentUserPrincipal();
        assertTrue(principal.isPresent());
        assertEquals("testuser", principal.get().getUsername());
    }

    @Test
    void testGetCurrentUserPrincipal_WhenNotAuthenticated() {
        Optional<UserPrincipal> principal = currentUserService.getCurrentUserPrincipal();
        assertTrue(principal.isEmpty());
    }

    @Test
    void testGetCurrentUser_FromPrincipal() {
        Authentication auth = new UsernamePasswordAuthenticationToken(testPrincipal, null, testPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));

        Optional<User> user = currentUserService.getCurrentUser();
        assertTrue(user.isPresent());
        assertEquals(100L, user.get().getId());
    }

    @Test
    void testGetCurrentEmployee_FromPrincipal() {
        Authentication auth = new UsernamePasswordAuthenticationToken(testPrincipal, null, testPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(employeeRepository.findByUserId(100L)).thenReturn(Optional.of(testEmployee));

        Optional<Employee> emp = currentUserService.getCurrentEmployee();
        assertTrue(emp.isPresent());
        assertEquals(200L, emp.get().getId());
        assertEquals(200L, currentUserService.getCurrentEmployeeId().orElseThrow());
    }

    @Test
    void testGetEmployeeIdForUser_WithRequestedId() {
        Long result = currentUserService.getEmployeeIdForUser(testPrincipal, 999L);
        assertEquals(999L, result);
    }

    @Test
    void testGetEmployeeIdForUser_WithoutRequestedId_UsesPrincipal() {
        when(employeeRepository.findByUserId(100L)).thenReturn(Optional.of(testEmployee));

        Long result = currentUserService.getEmployeeIdForUser(testPrincipal, null);
        assertEquals(200L, result);
    }

    @Test
    void testGetEmployeeIdForUser_WhenUnauthenticated_ThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> currentUserService.getEmployeeIdForUser(null, null));
    }

    @Test
    void testRoleChecks() {
        Authentication auth = new UsernamePasswordAuthenticationToken(testPrincipal, null, testPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertTrue(currentUserService.hasRole("ROLE_EMPLOYEE"));
        assertTrue(currentUserService.hasRole("EMPLOYEE"));
        assertFalse(currentUserService.hasRole("ADMIN"));
        assertTrue(currentUserService.isEmployee());
        assertFalse(currentUserService.isAdmin());
        assertFalse(currentUserService.isHrOrAdmin());
        assertTrue(currentUserService.hasAnyRole("ADMIN", "EMPLOYEE"));
    }
}
