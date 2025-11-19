package com.springpay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springpay.dto.request.MerchantRegistrationRequest;
import com.springpay.dto.response.MerchantRegistrationResponse;
import com.springpay.enums.MerchantStatus;
import com.springpay.exception.ConflictException;
import com.springpay.service.MerchantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MerchantController.
 * Uses @WebMvcTest to test the REST API endpoints with MockMvc.
 * Security filters are disabled to test controller logic in isolation.
 */
@WebMvcTest(value = MerchantController.class,
        excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.springpay\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class MerchantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MerchantService merchantService;

    private MerchantRegistrationRequest validRequest;
    private MerchantRegistrationResponse mockResponse;

    @BeforeEach
    void setUp() {
        validRequest = MerchantRegistrationRequest.builder()
                .name("Tulip Store")
                .email("merchant@tulipstore.com")
                .password("SecureP@ss123")
                .build();

        mockResponse = MerchantRegistrationResponse.builder()
                .id(1L)
                .name("Tulip Store")
                .email("merchant@tulipstore.com")
                .apiKey("sk_live_abc123xyz456def789ghi012jkl345mno678pqr901stu234vwx567yza890bcd123")
                .status(MerchantStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void registerMerchant_ValidRequest_Returns201Created() throws Exception {
        // Given
        when(merchantService.registerMerchant(any(MerchantRegistrationRequest.class)))
                .thenReturn(mockResponse);

        // When/Then
        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Tulip Store"))
                .andExpect(jsonPath("$.email").value("merchant@tulipstore.com"))
                .andExpect(jsonPath("$.apiKey").value(containsString("sk_live_")))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(merchantService).registerMerchant(any(MerchantRegistrationRequest.class));
    }

    @Test
    void registerMerchant_EmailAlreadyExists_Returns409Conflict() throws Exception {
        // Given
        when(merchantService.registerMerchant(any(MerchantRegistrationRequest.class)))
                .thenThrow(new ConflictException("Email already registered"));

        // When/Then
        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Email already registered"));

        verify(merchantService).registerMerchant(any(MerchantRegistrationRequest.class));
    }

    @Test
    void registerMerchant_MissingName_Returns400BadRequest() throws Exception {
        // Given
        MerchantRegistrationRequest invalidRequest = MerchantRegistrationRequest.builder()
                .name("")  // Empty name
                .email("merchant@tulipstore.com")
                .password("SecureP@ss123")
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(merchantService, never()).registerMerchant(any(MerchantRegistrationRequest.class));
    }

    @Test
    void registerMerchant_InvalidEmail_Returns400BadRequest() throws Exception {
        // Given
        MerchantRegistrationRequest invalidRequest = MerchantRegistrationRequest.builder()
                .name("Tulip Store")
                .email("invalid-email")  // Invalid email format
                .password("SecureP@ss123")
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(merchantService, never()).registerMerchant(any(MerchantRegistrationRequest.class));
    }

    @Test
    void registerMerchant_PasswordTooShort_Returns400BadRequest() throws Exception {
        // Given
        MerchantRegistrationRequest invalidRequest = MerchantRegistrationRequest.builder()
                .name("Tulip Store")
                .email("merchant@tulipstore.com")
                .password("Weak1!")  // Less than 8 characters
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(merchantService, never()).registerMerchant(any(MerchantRegistrationRequest.class));
    }

    @Test
    void registerMerchant_PasswordMissingUppercase_Returns400BadRequest() throws Exception {
        // Given
        MerchantRegistrationRequest invalidRequest = MerchantRegistrationRequest.builder()
                .name("Tulip Store")
                .email("merchant@tulipstore.com")
                .password("weakpass123!")  // No uppercase letter
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(merchantService, never()).registerMerchant(any(MerchantRegistrationRequest.class));
    }

    @Test
    void registerMerchant_PasswordMissingNumber_Returns400BadRequest() throws Exception {
        // Given
        MerchantRegistrationRequest invalidRequest = MerchantRegistrationRequest.builder()
                .name("Tulip Store")
                .email("merchant@tulipstore.com")
                .password("WeakPass!")  // No number
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(merchantService, never()).registerMerchant(any(MerchantRegistrationRequest.class));
    }

    @Test
    void registerMerchant_PasswordMissingSpecialChar_Returns400BadRequest() throws Exception {
        // Given
        MerchantRegistrationRequest invalidRequest = MerchantRegistrationRequest.builder()
                .name("Tulip Store")
                .email("merchant@tulipstore.com")
                .password("WeakPass123")  // No special character
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(merchantService, never()).registerMerchant(any(MerchantRegistrationRequest.class));
    }

    @Test
    void registerMerchant_NullEmail_Returns400BadRequest() throws Exception {
        // Given
        MerchantRegistrationRequest invalidRequest = MerchantRegistrationRequest.builder()
                .name("Tulip Store")
                .email(null)  // Null email
                .password("SecureP@ss123")
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(merchantService, never()).registerMerchant(any(MerchantRegistrationRequest.class));
    }

    @Test
    void registerMerchant_EmptyRequestBody_Returns400BadRequest() throws Exception {
        // When/Then
        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(merchantService, never()).registerMerchant(any(MerchantRegistrationRequest.class));
    }

    @Test
    void registerMerchant_MalformedJson_Returns400BadRequest() throws Exception {
        // When/Then
        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());

        verify(merchantService, never()).registerMerchant(any(MerchantRegistrationRequest.class));
    }
}
