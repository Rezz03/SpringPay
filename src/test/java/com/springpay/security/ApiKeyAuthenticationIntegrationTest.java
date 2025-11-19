package com.springpay.security;

import com.springpay.entity.Merchant;
import com.springpay.enums.MerchantStatus;
import com.springpay.repository.MerchantRepository;
import com.springpay.util.ApiKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for API key authentication.
 * Tests the full authentication flow with real database and security configuration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiKeyAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private ApiKeyGenerator apiKeyGenerator;

    private String plainApiKey;
    private Merchant approvedMerchant;
    private Merchant pendingMerchant;

    @BeforeEach
    void setUp() {
        // Generate API key
        plainApiKey = apiKeyGenerator.generateApiKey();
        String hashedApiKey = apiKeyGenerator.hashApiKey(plainApiKey);

        // Create approved merchant
        approvedMerchant = Merchant.builder()
                .name("Tulip Store")
                .email("merchant@tulipstore.com")
                .passwordHash("$2a$12$KIXn8TvLKGHQvF4kR0xZ1eGqU5Y7bZ1vK9Xb3pQ2wF8zJ5yH6tC7m")
                .apiKeyHash(hashedApiKey)
                .status(MerchantStatus.APPROVED)
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        approvedMerchant = merchantRepository.save(approvedMerchant);

        // Create pending merchant with different API key
        String pendingApiKey = apiKeyGenerator.generateApiKey();
        String pendingHashedKey = apiKeyGenerator.hashApiKey(pendingApiKey);

        pendingMerchant = Merchant.builder()
                .name("Pending Store")
                .email("pending@example.com")
                .passwordHash("$2a$12$KIXn8TvLKGHQvF4kR0xZ1eGqU5Y7bZ1vK9Xb3pQ2wF8zJ5yH6tC7m")
                .apiKeyHash(pendingHashedKey)
                .status(MerchantStatus.PENDING)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        pendingMerchant = merchantRepository.save(pendingMerchant);
    }

    @Test
    void getProfile_WithValidApiKey_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/profile")
                        .header("Authorization", "ApiKey " + plainApiKey))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id", is(approvedMerchant.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Tulip Store")))
                .andExpect(jsonPath("$.email", is("merchant@tulipstore.com")))
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.emailVerified", is(true)));
    }

    @Test
    void getProfile_WithoutAuthorizationHeader_Returns403() throws Exception {
        // Spring Security returns 403 Forbidden when authentication is missing
        mockMvc.perform(get("/api/v1/merchants/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProfile_WithInvalidApiKey_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/profile")
                        .header("Authorization", "ApiKey sk_live_invalidkey123"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", is("Invalid or missing API key")));
    }

    @Test
    void getProfile_WithMalformedHeader_Returns403() throws Exception {
        // When Authorization header doesn't start with "ApiKey ", our filter skips it
        // and Spring Security returns 403 Forbidden due to lack of authentication
        mockMvc.perform(get("/api/v1/merchants/profile")
                        .header("Authorization", "Bearer " + plainApiKey))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProfile_WithEmptyApiKey_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/profile")
                        .header("Authorization", "ApiKey "))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", is("API key is empty")));
    }

}
