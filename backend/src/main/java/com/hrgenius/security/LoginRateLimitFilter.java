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
        String path = request.getServletPath();
        String uri = request.getRequestURI();
        boolean isLoginPath = (path != null && path.endsWith("/auth/login")) || (uri != null && uri.endsWith("/auth/login"));

        if ("POST".equalsIgnoreCase(request.getMethod()) && isLoginPath) {
            String clientIp = getClientIp(request);
            if (!rateLimiter.tryAcquire(clientIp)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many login attempts. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
