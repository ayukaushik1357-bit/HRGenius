package com.hrgenius.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginRateLimiter rateLimiter;

    public LoginRateLimitFilter() {
        this.rateLimiter = new LoginRateLimiter();
    }

    public LoginRateLimitFilter(LoginRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter != null ? rateLimiter : new LoginRateLimiter();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String endpoint = rateLimitedEndpoint(request);
        if ("POST".equalsIgnoreCase(request.getMethod()) && endpoint != null) {
            // Separate budget per endpoint so password-reset requests cannot lock out logins, and vice versa
            String key = request.getRemoteAddr() + "|" + endpoint;
            if (!rateLimiter.tryAcquire(key)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many attempts. Please try again in a minute.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Public auth endpoints that are throttled per client IP.
     * The IP is request.getRemoteAddr(): with server.forward-headers-strategy=native, Tomcat only
     * honours X-Forwarded-For from trusted internal proxies, so a client cannot fake its address.
     */
    private static final String[] THROTTLED = {
            "/auth/login", "/auth/register", "/auth/google", "/auth/password-reset/request"
    };

    private String rateLimitedEndpoint(HttpServletRequest request) {
        String path = request.getServletPath();
        String uri = request.getRequestURI();
        for (String endpoint : THROTTLED) {
            if ((path != null && path.endsWith(endpoint)) || (uri != null && uri.endsWith(endpoint))) {
                return endpoint;
            }
        }
        return null;
    }
}
