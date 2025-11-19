package com.springpay.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the SpringPay application.
 * Configures API key-based authentication and endpoint authorization.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiKeyAuthFilter apiKeyAuthFilter;

    /**
     * Configures the security filter chain.
     *
     * Configuration:
     * - Disables CSRF (not needed for REST API with API key auth)
     * - Sets session management to STATELESS (no sessions)
     * - Configures public endpoints (registration, login, Swagger UI)
     * - Requires API key authentication for all other endpoints
     * - Adds API key authentication filter
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (not needed for REST API)
                .csrf(AbstractHttpConfigurer::disable)

                // Configure session management (stateless for REST API)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers(HttpMethod.POST, "/api/v1/merchants/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()

                        // Admin endpoints (public for MVP - would require admin role in production)
                        .requestMatchers("/api/v1/admin/**").permitAll()

                        // Swagger/OpenAPI documentation (public for development)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Actuator endpoints (public for health checks)
                        .requestMatchers("/actuator/health").permitAll()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Disable form login (not needed for REST API)
                .formLogin(AbstractHttpConfigurer::disable)

                // Disable HTTP Basic authentication (we use API key)
                .httpBasic(AbstractHttpConfigurer::disable)

                // Add API key authentication filter
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
