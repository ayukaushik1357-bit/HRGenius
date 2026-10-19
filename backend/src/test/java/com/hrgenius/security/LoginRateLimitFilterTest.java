package com.hrgenius.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginRateLimitFilterTest {

    private LoginRateLimiter rateLimiter;
    private LoginRateLimitFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        // Limit to 3 attempts per minute for testing
        rateLimiter = new LoginRateLimiter(3, 60);
        filter = new LoginRateLimitFilter(rateLimiter);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void testRequestsUnderLimitPassThrough() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/auth/login");
        request.setRemoteAddr("192.168.1.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void testRequestsExceedingLimitReturn429() throws ServletException, IOException {
        String clientIp = "192.168.1.50";

        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setServletPath("/auth/login");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, filterChain);
            assertEquals(200, res.getStatus());
        }

        // 4th request from same IP should be blocked with 429
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedReq.setServletPath("/auth/login");
        blockedReq.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();

        filter.doFilter(blockedReq, blockedRes, filterChain);

        assertEquals(429, blockedRes.getStatus());
        assertTrue(blockedRes.getContentAsString().contains("\"status\":429"));
        assertTrue(blockedRes.getContentAsString().contains("Too many attempts"));

        // Another IP should still be allowed
        MockHttpServletRequest otherIpReq = new MockHttpServletRequest("POST", "/api/auth/login");
        otherIpReq.setServletPath("/auth/login");
        otherIpReq.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse otherIpRes = new MockHttpServletResponse();

        filter.doFilter(otherIpReq, otherIpRes, filterChain);
        assertEquals(200, otherIpRes.getStatus());
    }

    @Test
    void testNonLoginRequestsAreNotRateLimited() throws ServletException, IOException {
        String clientIp = "192.168.1.99";

        // Non-login endpoint
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/refresh");
            req.setServletPath("/auth/refresh");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, filterChain);
            assertEquals(200, res.getStatus());
        }
    }

    @Test
    void testSpoofedXForwardedForCannotBypassLimit() throws ServletException, IOException {
        // An attacker rotating fake X-Forwarded-For values must still be limited by the real connection IP
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setServletPath("/auth/login");
            req.setRemoteAddr("198.51.100.7");
            req.addHeader("X-Forwarded-For", "203.0.113." + i);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, filterChain);
            assertEquals(200, res.getStatus());
        }

        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedReq.setServletPath("/auth/login");
        blockedReq.setRemoteAddr("198.51.100.7");
        blockedReq.addHeader("X-Forwarded-For", "203.0.113.99");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();

        filter.doFilter(blockedReq, blockedRes, filterChain);
        assertEquals(429, blockedRes.getStatus());
    }

    @Test
    void testPasswordResetRequestsAreThrottledSeparatelyFromLogin() throws ServletException, IOException {
        for (int i = 0; i < 4; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/password-reset/request");
            req.setServletPath("/auth/password-reset/request");
            req.setRemoteAddr("198.51.100.8");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, filterChain);
            assertEquals(i < 3 ? 200 : 429, res.getStatus());
        }
        // Login from the same IP still has its own budget
        MockHttpServletRequest login = new MockHttpServletRequest("POST", "/api/auth/login");
        login.setServletPath("/auth/login");
        login.setRemoteAddr("198.51.100.8");
        MockHttpServletResponse loginRes = new MockHttpServletResponse();
        filter.doFilter(login, loginRes, filterChain);
        assertEquals(200, loginRes.getStatus());
    }
}
