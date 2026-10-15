package com.hrgenius.service;

import com.hrgenius.dto.auth.LoginRequest;
import com.hrgenius.entity.User;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.repository.UserRepository;
import com.hrgenius.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceLockoutTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@hrgenius.com");
        user.setFailedAttempts(0);
        user.setLockoutTime(null);
    }

    @Test
    void testFailedLoginIncrementsAttempts() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("WrongPassword");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.login(req, response));
        assertTrue(ex.getMessage().contains("Remaining attempts before account lockout: 4"));
        assertEquals(1, user.getFailedAttempts());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testAccountLockoutAfterFiveAttempts() {
        user.setFailedAttempts(4);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("WrongPassword");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.login(req, response));
        assertTrue(ex.getMessage().contains("Account locked for 15 minutes"));
        assertEquals(5, user.getFailedAttempts());
        assertNotNull(user.getLockoutTime());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testLockedAccountRejectsLoginImmediately() {
        user.setLockoutTime(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("AnyPassword");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.login(req, response));
        assertTrue(ex.getMessage().contains("Account is locked"));
        verifyNoInteractions(authenticationManager);
    }
}
