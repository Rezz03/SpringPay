package com.springpay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springpay.dto.request.GenerateApiKeyRequest;
import com.springpay.dto.response.ApiKeyResponse;
import com.springpay.entity.ApiKey;
import com.springpay.entity.Merchant;
import com.springpay.enums.MerchantStatus;
import com.springpay.exception.ForbiddenException;
import com.springpay.exception.NotFoundException;
import com.springpay.exception.UnauthorizedException;
import com.springpay.security.ApiKeyAuthenticationToken;
import com.springpay.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ApiKeyController.
 * Tests all REST endpoints for API key management.
 */
@WebMvcTest(controllers = ApiKeyController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApiKeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApiKeyService apiKeyService;

    private Merchant testMerchant;
    private ApiKeyAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        testMerchant = Merchant.builder()
                .id(1L)
                .name("Test Merchant")
                .email("test@merchant.com")
                .status(MerchantStatus.APPROVED)
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        authentication = new ApiKeyAuthenticationToken(
                "sk_live_test123",
                testMerchant,
                Collections.emptyList()
        );
    }

    // ==================== POST /api/v1/keys (Generate API Key) Tests ====================

    @Test
    void generateApiKey_ValidRequest_Returns201() throws Exception {
        // Given
        GenerateApiKeyRequest request = GenerateApiKeyRequest.builder()
                .label("Production Key")
                .build();

        ApiKey apiKey = ApiKey.builder()
                .id(10L)
                .merchant(testMerchant)
                .keyHash("hash123")
                .label("Production Key")
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        String plainTextKey = "sk_live_newkey123456789";

        when(apiKeyService.generateAdditionalKey(eq(1L), eq("Production Key")))
                .thenReturn(new Object[]{apiKey, plainTextKey});

        // When/Then
        mockMvc.perform(post("/api/v1/keys")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.apiKey").value(plainTextKey))
                .andExpect(jsonPath("$.label").value("Production Key"))
                .andExpect(jsonPath("$.revoked").value(false))
                .andExpect(jsonPath("$.warning").value(containsString("only be shown once")));
    }

    @Test
    void generateApiKey_MissingLabel_Returns400() throws Exception {
        // Given
        GenerateApiKeyRequest request = GenerateApiKeyRequest.builder()
                .label("") // Invalid: empty label
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/keys")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateApiKey_LabelTooLong_Returns400() throws Exception {
        // Given
        String longLabel = "a".repeat(101); // 101 characters, exceeds max of 100
        GenerateApiKeyRequest request = GenerateApiKeyRequest.builder()
                .label(longLabel)
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/keys")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateApiKey_MerchantNotApproved_Returns401() throws Exception {
        // Given
        GenerateApiKeyRequest request = GenerateApiKeyRequest.builder()
                .label("Production Key")
                .build();

        when(apiKeyService.generateAdditionalKey(anyLong(), anyString()))
                .thenThrow(new UnauthorizedException("Only approved merchants can generate API keys"));

        // When/Then
        mockMvc.perform(post("/api/v1/keys")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(containsString("approved merchants")));
    }

    @Test
    void generateApiKey_NoAuthentication_Returns401() throws Exception {
        // Given
        GenerateApiKeyRequest request = GenerateApiKeyRequest.builder()
                .label("Production Key")
                .build();

        // When/Then - No authentication provided
        mockMvc.perform(post("/api/v1/keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET /api/v1/keys (List API Keys) Tests ====================

    @Test
    void listApiKeys_ReturnsAllKeys() throws Exception {
        // Given
        ApiKey key1 = ApiKey.builder()
                .id(1L)
                .merchant(testMerchant)
                .label("Production Key")
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .build();

        ApiKey key2 = ApiKey.builder()
                .id(2L)
                .merchant(testMerchant)
                .label("Test Key")
                .revoked(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        List<ApiKey> keys = Arrays.asList(key1, key2);

        when(apiKeyService.listApiKeys(1L)).thenReturn(keys);

        // When/Then
        mockMvc.perform(get("/api/v1/keys")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].label").value("Production Key"))
                .andExpect(jsonPath("$[0].revoked").value(false))
                .andExpect(jsonPath("$[0].apiKey").doesNotExist()) // Plain-text key never included
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].label").value("Test Key"))
                .andExpect(jsonPath("$[1].revoked").value(true));
    }

    @Test
    void listApiKeys_EmptyList_ReturnsEmptyArray() throws Exception {
        // Given
        when(apiKeyService.listApiKeys(1L)).thenReturn(Arrays.asList());

        // When/Then
        mockMvc.perform(get("/api/v1/keys")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listApiKeys_NoAuthentication_Returns401() throws Exception {
        // When/Then - No authentication provided
        mockMvc.perform(get("/api/v1/keys"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== DELETE /api/v1/keys/{keyId} (Revoke API Key) Tests ====================

    @Test
    void revokeApiKey_ValidKey_Returns200() throws Exception {
        // Given
        Long keyId = 10L;

        doNothing().when(apiKeyService).revokeApiKey(eq(1L), eq(keyId));

        // When/Then
        mockMvc.perform(delete("/api/v1/keys/{keyId}", keyId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("revoked successfully")));
    }

    @Test
    void revokeApiKey_KeyNotFound_Returns404() throws Exception {
        // Given
        Long keyId = 999L;

        doThrow(new NotFoundException("API key not found with ID: " + keyId))
                .when(apiKeyService).revokeApiKey(eq(1L), eq(keyId));

        // When/Then
        mockMvc.perform(delete("/api/v1/keys/{keyId}", keyId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(containsString("not found")));
    }

    @Test
    void revokeApiKey_WrongMerchant_Returns403() throws Exception {
        // Given
        Long keyId = 10L;

        doThrow(new ForbiddenException("You do not have permission to revoke this API key"))
                .when(apiKeyService).revokeApiKey(eq(1L), eq(keyId));

        // When/Then
        mockMvc.perform(delete("/api/v1/keys/{keyId}", keyId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(containsString("permission")));
    }

    @Test
    void revokeApiKey_NoAuthentication_Returns401() throws Exception {
        // Given
        Long keyId = 10L;

        // When/Then - No authentication provided
        mockMvc.perform(delete("/api/v1/keys/{keyId}", keyId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokeApiKey_InvalidKeyIdFormat_Returns400() throws Exception {
        // When/Then - Invalid path parameter (not a number)
        mockMvc.perform(delete("/api/v1/keys/{keyId}", "invalid")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication)))
                .andExpect(status().isBadRequest());
    }
}
