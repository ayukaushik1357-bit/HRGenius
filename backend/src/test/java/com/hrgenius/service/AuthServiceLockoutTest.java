package com.hrgenius.service;

import com.hrgenius.dto.auth.AuthResponse;
import com.hrgenius.dto.auth.LoginRequest;
import com.hrgenius.entity.RefreshToken;
import com.hrgenius.entity.Role;
import com.hrgenius.entity.User;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.repository.RefreshTokenRepository;
import com.hrgenius.repository.UserRepository;
import com.hrgenius.security.JwtTokenProvider;
import com.hrgenius.security.UserPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

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
    private RefreshTokenRepository refreshTokenRepository;
    @Spy
    private com.hrgenius.security.CookieProperties cookieProperties = new com.hrgenius.security.CookieProperties();
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
        user.setPassword("password");
        user.setEnabled(true);
        Role role = new Role(Role.RoleName.EMPLOYEE);
        user.setRoles(Set.of(role));
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

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(req, response));
        assertEquals("Invalid username or password", ex.getMessage());
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

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(req, response));
        assertEquals("Invalid username or password", ex.getMessage());
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

        LockedException ex = assertThrows(LockedException.class, () -> authService.login(req, response));
        assertTrue(ex.getMessage().contains("Account is temporarily locked"));
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void testFailedLoginNonExistentUserReturnsSameGenericError() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest req = new LoginRequest();
        req.setUsername("unknown");
        req.setPassword("WrongPassword");

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(req, response));
        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void testLoginSavesHashedRefreshToken() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenProvider.generateAccessToken(auth)).thenReturn("sample-access-token");
        when(tokenProvider.generateRefreshToken(auth)).thenReturn("sample-refresh-token");
        when(tokenProvider.getJtiFromToken("sample-refresh-token")).thenReturn("jti-123");
        when(tokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("GoodPassword");

        AuthResponse resp = authService.login(req, response);

        assertNotNull(resp);
        assertEquals("sample-access-token", resp.getAccessToken());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(1)).save(captor.capture());
        RefreshToken savedToken = captor.getValue();
        assertEquals(AuthService.hashToken("sample-refresh-token"), savedToken.getTokenHash());
        assertEquals("jti-123", savedToken.getJti());
        assertFalse(savedToken.isRevoked());
        assertEquals(user, savedToken.getUser());
    }

    @Test
    void testRefreshTokenRejectsAccessToken() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        Cookie cookie = new Cookie("hrgenius_refresh_token", "sample-access-token");
        when(req.getCookies()).thenReturn(new Cookie[]{cookie});
        when(tokenProvider.validateToken("sample-access-token")).thenReturn(true);
        when(tokenProvider.getTokenType("sample-access-token")).thenReturn("access");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.refreshToken(req, response));
        assertTrue(ex.getMessage().contains("Invalid token type"));
    }

    @Test
    void testRefreshTokenRotationSuccess() {
        String rawToken = "valid-refresh-token";
        String tokenHash = AuthService.hashToken(rawToken);

        RefreshToken dbToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .jti("old-jti")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .revoked(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        HttpServletRequest req = mock(HttpServletRequest.class);
        Cookie cookie = new Cookie("hrgenius_refresh_token", rawToken);
        when(req.getCookies()).thenReturn(new Cookie[]{cookie});
        when(tokenProvider.validateToken(rawToken)).thenReturn(true);
        when(tokenProvider.getTokenType(rawToken)).thenReturn("refresh");
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(dbToken));

        UserPrincipal principal = new UserPrincipal(user);
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(principal);
        when(tokenProvider.generateAccessTokenFromUsername(eq("testuser"), any())).thenReturn("new-access-token");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("new-refresh-token");
        when(tokenProvider.getJtiFromToken("new-refresh-token")).thenReturn("new-jti");
        when(tokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);

        AuthResponse res = authService.refreshToken(req, response);

        assertNotNull(res);
        assertEquals("new-access-token", res.getAccessToken());

        // Old token should be revoked and replaced_by populated
        assertTrue(dbToken.isRevoked());
        assertEquals(AuthService.hashToken("new-refresh-token"), dbToken.getReplacedBy());

        // New token saved
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());
        RefreshToken newlySaved = captor.getAllValues().get(1);
        assertEquals(AuthService.hashToken("new-refresh-token"), newlySaved.getTokenHash());
        assertEquals("new-jti", newlySaved.getJti());
        assertFalse(newlySaved.isRevoked());
    }

    @Test
    void testRefreshTokenReuseDetectionRevokesAllTokens() {
        String rawToken = "stolen-reused-token";
        String tokenHash = AuthService.hashToken(rawToken);

        RefreshToken revokedToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .jti("stolen-jti")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .revoked(true) // ALREADY REVOKED!
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();

        HttpServletRequest req = mock(HttpServletRequest.class);
        Cookie cookie = new Cookie("hrgenius_refresh_token", rawToken);
        when(req.getCookies()).thenReturn(new Cookie[]{cookie});
        when(tokenProvider.validateToken(rawToken)).thenReturn(true);
        when(tokenProvider.getTokenType(rawToken)).thenReturn("refresh");
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedToken));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.refreshToken(req, response));
        assertTrue(ex.getMessage().contains("reused revoked token"));

        // Must revoke all tokens of that user!
        verify(refreshTokenRepository, times(1)).revokeAllByUser(user);
    }

    @Test
    void testLogoutRevokesToken() {
        String rawToken = "token-to-logout";
        String tokenHash = AuthService.hashToken(rawToken);

        RefreshToken dbToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .revoked(false)
                .build();

        HttpServletRequest req = mock(HttpServletRequest.class);
        Cookie cookie = new Cookie("hrgenius_refresh_token", rawToken);
        when(req.getCookies()).thenReturn(new Cookie[]{cookie});
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(dbToken));

        authService.logout(req, response);

        assertTrue(dbToken.isRevoked());
        verify(refreshTokenRepository, times(1)).save(dbToken);
    }

    @Test
    void testLogoutAllRevokesAllTokens() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        authService.logoutAll("testuser", response);

        verify(refreshTokenRepository, times(1)).revokeAllByUser(user);
    }

    @Test
    void testCookieFlagsDev() {
        when(tokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        cookieProperties.setSecure(false);
        cookieProperties.setSameSite("Lax");
        cookieProperties.setPath("/api/auth");

        authService.addRefreshTokenCookie(response, "test-refresh-token");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), captor.capture());
        String cookieHeader = captor.getValue();

        assertTrue(cookieHeader.contains("Path=/api/auth"));
        assertTrue(cookieHeader.contains("SameSite=Lax"));
        assertTrue(cookieHeader.contains("HttpOnly"));
        assertFalse(cookieHeader.contains("Secure"));
    }

    @Test
    void testCookieFlagsProd() {
        when(tokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        cookieProperties.setSecure(true);
        cookieProperties.setSameSite("Strict");
        cookieProperties.setPath("/api/auth");

        authService.addRefreshTokenCookie(response, "test-refresh-token");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), captor.capture());
        String cookieHeader = captor.getValue();

        assertTrue(cookieHeader.contains("Path=/api/auth"));
        assertTrue(cookieHeader.contains("SameSite=Strict"));
        assertTrue(cookieHeader.contains("HttpOnly"));
        assertTrue(cookieHeader.contains("Secure"));
    }
}
