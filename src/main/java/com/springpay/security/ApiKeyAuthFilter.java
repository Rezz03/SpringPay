package com.springpay.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springpay.entity.Merchant;
import com.springpay.exception.ErrorResponse;
import com.springpay.exception.UnauthorizedException;
import com.springpay.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * Spring Security filter for API key-based authentication.
 * Intercepts requests and validates the API key in the Authorization header.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    /**
     * Filters incoming requests and validates API keys.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        // If no Authorization header, continue without authentication
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // If Authorization header doesn't start with "ApiKey ", skip this filter
        if (!authorizationHeader.startsWith("ApiKey ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract and validate API key
            String apiKey = apiKeyService.extractApiKey(authorizationHeader);
            Merchant merchant = apiKeyService.validateApiKey(apiKey);

            // Create authentication token and set in security context
            ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(
                    apiKey,
                    merchant,
                    Collections.emptyList()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("API key authentication successful for merchant ID: {}", merchant.getId());

            // Continue with the filter chain
            filterChain.doFilter(request, response);

        } catch (UnauthorizedException ex) {
            // Handle authentication failure
            log.warn("API key authentication failed: {}", ex.getMessage());
            handleAuthenticationFailure(response, ex.getMessage());
        }
    }

    /**
     * Handles authentication failures by sending a 401 Unauthorized response.
     *
     * @param response the HTTP response
     * @param message the error message
     * @throws IOException if writing to response fails
     */
    private void handleAuthenticationFailure(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("UNAUTHORIZED")
                .message(message)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
