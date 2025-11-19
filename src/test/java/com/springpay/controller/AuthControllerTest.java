package com.springpay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springpay.dto.request.LoginRequest;
import com.springpay.entity.Merchant;
import com.springpay.enums.MerchantStatus;
import com.springpay.exception.UnauthorizedException;
import com.springpay.service.MerchantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.springpay.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * Tests REST endpoints with mocked service layer.
 */
@WebMvcTest(value = AuthController.class,
        excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.springpay\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MerchantService merchantService;

    private LoginRequest validLoginRequest;
    private Merchant mockMerchant;

    @BeforeEach
    void setUp() {
        validLoginRequest = LoginRequest.builder()
                .email("merchant@tulipstore.com")
                .password("SecureP@ss123")
                .build();

        mockMerchant = Merchant.builder()
                .id(1L)
                .name("Tulip Store")
                .email("merchant@tulipstore.com")
                .passwordHash("$2a$12$KIXn8TvLKGHQvF4kR0xZ1eGqU5Y7bZ1vK9Xb3pQ2wF8zJ5yH6tC7m")
                .apiKeyHash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .status(MerchantStatus.APPROVED)
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void login_ValidCredentials_Returns200WithMerchantDetails() throws Exception {
        // Given
        when(merchantService.login(validLoginRequest.getEmail(), validLoginRequest.getPassword()))
                .thenReturn(mockMerchant);

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Tulip Store")))
                .andExpect(jsonPath("$.email", is("merchant@tulipstore.com")))
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.emailVerified", is(true)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.apiKey").doesNotExist()); // API key should NOT be in login response
    }

    @Test
    void login_InvalidCredentials_Returns401() throws Exception {
        // Given
        when(merchantService.login(validLoginRequest.getEmail(), validLoginRequest.getPassword()))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    void login_MissingEmail_Returns400() throws Exception {
        // Given
        LoginRequest invalidRequest = LoginRequest.builder()
                .email(null)
                .password("SecureP@ss123")
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void login_MissingPassword_Returns400() throws Exception {
        // Given
        LoginRequest invalidRequest = LoginRequest.builder()
                .email("merchant@tulipstore.com")
                .password(null)
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void login_InvalidEmailFormat_Returns400() throws Exception {
        // Given
        LoginRequest invalidRequest = LoginRequest.builder()
                .email("invalid-email")
                .password("SecureP@ss123")
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", is("Request validation failed")));
    }

    @Test
    void login_PasswordTooShort_Returns400() throws Exception {
        // Given
        LoginRequest invalidRequest = LoginRequest.builder()
                .email("merchant@tulipstore.com")
                .password("Short1!")
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", is("Request validation failed")));
    }

    @Test
    void login_EmptyRequestBody_Returns400() throws Exception {
        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void login_MalformedJson_Returns400() throws Exception {
        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("MALFORMED_REQUEST")));
    }

    @Test
    void login_ValidCredentials_DoesNotReturnPasswordHash() throws Exception {
        // Given
        when(merchantService.login(validLoginRequest.getEmail(), validLoginRequest.getPassword()))
                .thenReturn(mockMerchant);

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void login_ValidCredentials_DoesNotReturnApiKeyHash() throws Exception {
        // Given
        when(merchantService.login(validLoginRequest.getEmail(), validLoginRequest.getPassword()))
                .thenReturn(mockMerchant);

        // When/Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKeyHash").doesNotExist())
                .andExpect(jsonPath("$.apiKey").doesNotExist());
    }
}
