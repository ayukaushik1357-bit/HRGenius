package com.hrgenius.security;

import com.hrgenius.entity.Role;
import com.hrgenius.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        jwtProperties.setAccessTokenExpirationMs(900000); // 15 mins
        jwtProperties.setRefreshTokenExpirationMs(604800000); // 7 days
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    private UserPrincipal createMockUserPrincipal(String username) {
        User user = new User();
        user.setId(10L);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("password");
        user.setEnabled(true);
        Role role = new Role(Role.RoleName.EMPLOYEE);
        user.setRoles(Set.of(role));
        return new UserPrincipal(user);
    }

    @Test
    void testTokenTypesAndJtiClaims() {
        UserPrincipal principal = createMockUserPrincipal("alice");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);

        String accessToken = jwtTokenProvider.generateAccessToken(auth);
        String refreshToken = jwtTokenProvider.generateRefreshToken(auth);

        assertNotNull(accessToken);
        assertNotNull(refreshToken);

        // Validate types
        assertEquals("access", jwtTokenProvider.getTokenType(accessToken));
        assertEquals("refresh", jwtTokenProvider.getTokenType(refreshToken));
        assertTrue(jwtTokenProvider.isAccessToken(accessToken));
        assertFalse(jwtTokenProvider.isRefreshToken(accessToken));
        assertTrue(jwtTokenProvider.isRefreshToken(refreshToken));
        assertFalse(jwtTokenProvider.isAccessToken(refreshToken));

        // Validate JTIs
        String accessJti = jwtTokenProvider.getJtiFromToken(accessToken);
        String refreshJti = jwtTokenProvider.getJtiFromToken(refreshToken);

        assertNotNull(accessJti);
        assertNotNull(refreshJti);
        assertNotEquals(accessJti, refreshJti);

        // Successive tokens have distinct JTIs
        String secondAccessToken = jwtTokenProvider.generateAccessToken(auth);
        assertNotEquals(accessJti, jwtTokenProvider.getJtiFromToken(secondAccessToken));
    }

    @Test
    void testJwtAuthenticationFilterRejectsRefreshToken() throws Exception {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);

        UserPrincipal principal = createMockUserPrincipal("bob");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);

        String refreshToken = jwtTokenProvider.generateRefreshToken(auth);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + refreshToken);

        filter.doFilter(request, response, filterChain);

        // Refresh token must NOT set authentication
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testJwtAuthenticationFilterAcceptsAccessToken() throws Exception {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);

        UserPrincipal principal = createMockUserPrincipal("carol");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);

        String accessToken = jwtTokenProvider.generateAccessToken(auth);

        UserDetails userDetails = principal;
        when(userDetailsService.loadUserByUsername("carol")).thenReturn(userDetails);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + accessToken);

        filter.doFilter(request, response, filterChain);

        // Access token MUST set authentication
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("carol", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(userDetailsService, times(1)).loadUserByUsername("carol");
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
