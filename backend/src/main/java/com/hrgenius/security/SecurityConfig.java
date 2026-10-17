package com.hrgenius.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;
    private final Environment environment;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          @Autowired(required = false) LoginRateLimitFilter loginRateLimitFilter,
                          @Autowired(required = false) Environment environment) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.loginRateLimitFilter = loginRateLimitFilter;
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean isH2 = environment != null && environment.acceptsProfiles(Profiles.of("h2"));
        boolean isDevOrH2 = environment != null && environment.acceptsProfiles(Profiles.of("h2", "dev"));

        http
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                })
            )
            .authorizeHttpRequests(auth -> {
                // Public auth endpoints
                auth.requestMatchers("/auth/**").permitAll();
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                // H2 console: permitAll and frame options only in h2 profile; never in dev/prod
                if (isH2) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                } else {
                    auth.requestMatchers("/h2-console/**").denyAll();
                }

                // Swagger: available only in h2/dev profiles (or ADMIN-only in prod)
                if (isDevOrH2) {
                    auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                } else {
                    auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasRole("ADMIN");
                }

                // Everything else requires authentication
                auth.anyRequest().authenticated();
            });

        // Frame options: sameOrigin only in h2 profile; deny in dev/prod
        if (isH2) {
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        } else {
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny));
        }

        if (loginRateLimitFilter != null) {
            http.addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        }
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
