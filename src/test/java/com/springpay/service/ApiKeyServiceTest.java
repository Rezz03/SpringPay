package com.springpay.service;

import com.springpay.entity.Merchant;
import com.springpay.enums.MerchantStatus;
import com.springpay.exception.UnauthorizedException;
import com.springpay.repository.MerchantRepository;
import com.springpay.util.ApiKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApiKeyService.
 * Tests API key validation and extraction logic.
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private ApiKeyGenerator apiKeyGenerator;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private String plainApiKey;
    private String hashedApiKey;
    private Merchant approvedMerchant;
    private Merchant pendingMerchant;
    private Merchant suspendedMerchant;

    @BeforeEach
    void setUp() {
        plainApiKey = "sk_live_abc123xyz456def789ghi012jkl345mno678pqr901stu234vwx567yza890bcd123";
        hashedApiKey = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        approvedMerchant = Merchant.builder()
                .id(1L)
                .name("Tulip Store")
                .email("merchant@tulipstore.com")
                .passwordHash("$2a$12$KIXn8TvLKGHQvF4kR0xZ1eGqU5Y7bZ1vK9Xb3pQ2wF8zJ5yH6tC7m")
                .apiKeyHash(hashedApiKey)
                .status(MerchantStatus.APPROVED)
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        pendingMerchant = Merchant.builder()
                .id(2L)
                .name("Pending Store")
                .email("pending@example.com")
                .passwordHash("$2a$12$KIXn8TvLKGHQvF4kR0xZ1eGqU5Y7bZ1vK9Xb3pQ2wF8zJ5yH6tC7m")
                .apiKeyHash(hashedApiKey)
                .status(MerchantStatus.PENDING)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        suspendedMerchant = Merchant.builder()
                .id(3L)
                .name("Suspended Store")
                .email("suspended@example.com")
                .passwordHash("$2a$12$KIXn8TvLKGHQvF4kR0xZ1eGqU5Y7bZ1vK9Xb3pQ2wF8zJ5yH6tC7m")
                .apiKeyHash(hashedApiKey)
                .status(MerchantStatus.SUSPENDED)
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== validateApiKey Tests ====================

    @Test
    void validateApiKey_ValidKeyAndApprovedMerchant_ReturnsMerchant() {
        // Given
        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(merchantRepository.findByApiKeyHash(hashedApiKey)).thenReturn(Optional.of(approvedMerchant));

        // When
        Merchant result = apiKeyService.validateApiKey(plainApiKey);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("merchant@tulipstore.com");
        assertThat(result.getStatus()).isEqualTo(MerchantStatus.APPROVED);

        verify(apiKeyGenerator).hashApiKey(plainApiKey);
        verify(merchantRepository).findByApiKeyHash(hashedApiKey);
    }

    @Test
    void validateApiKey_InvalidKey_ThrowsUnauthorizedException() {
        // Given
        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(merchantRepository.findByApiKeyHash(hashedApiKey)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> apiKeyService.validateApiKey(plainApiKey))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or missing API key");

        verify(apiKeyGenerator).hashApiKey(plainApiKey);
        verify(merchantRepository).findByApiKeyHash(hashedApiKey);
    }

    @Test
    void validateApiKey_NullKey_ThrowsUnauthorizedException() {
        // When/Then
        assertThatThrownBy(() -> apiKeyService.validateApiKey(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or missing API key");

        verify(apiKeyGenerator, never()).hashApiKey(anyString());
        verify(merchantRepository, never()).findByApiKeyHash(anyString());
    }

    @Test
    void validateApiKey_EmptyKey_ThrowsUnauthorizedException() {
        // When/Then
        assertThatThrownBy(() -> apiKeyService.validateApiKey(""))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or missing API key");

        verify(apiKeyGenerator, never()).hashApiKey(anyString());
        verify(merchantRepository, never()).findByApiKeyHash(anyString());
    }

    @Test
    void validateApiKey_BlankKey_ThrowsUnauthorizedException() {
        // When/Then
        assertThatThrownBy(() -> apiKeyService.validateApiKey("   "))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or missing API key");

        verify(apiKeyGenerator, never()).hashApiKey(anyString());
        verify(merchantRepository, never()).findByApiKeyHash(anyString());
    }

    @Test
    void validateApiKey_PendingMerchant_ThrowsUnauthorizedException() {
        // Given
        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(merchantRepository.findByApiKeyHash(hashedApiKey)).thenReturn(Optional.of(pendingMerchant));

        // When/Then
        assertThatThrownBy(() -> apiKeyService.validateApiKey(plainApiKey))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Merchant account is not approved");

        verify(apiKeyGenerator).hashApiKey(plainApiKey);
        verify(merchantRepository).findByApiKeyHash(hashedApiKey);
    }

    @Test
    void validateApiKey_SuspendedMerchant_ThrowsUnauthorizedException() {
        // Given
        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(merchantRepository.findByApiKeyHash(hashedApiKey)).thenReturn(Optional.of(suspendedMerchant));

        // When/Then
        assertThatThrownBy(() -> apiKeyService.validateApiKey(plainApiKey))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Merchant account is not approved");

        verify(apiKeyGenerator).hashApiKey(plainApiKey);
        verify(merchantRepository).findByApiKeyHash(hashedApiKey);
    }

    // ==================== extractApiKey Tests ====================

    @Test
    void extractApiKey_ValidHeader_ReturnsApiKey() {
        // Given
        String authHeader = "ApiKey " + plainApiKey;

        // When
        String result = apiKeyService.extractApiKey(authHeader);

        // Then
        assertThat(result).isEqualTo(plainApiKey);
    }

    @Test
    void extractApiKey_NullHeader_ThrowsUnauthorizedException() {
        // When/Then
        assertThatThrownBy(() -> apiKeyService.extractApiKey(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Missing Authorization header");
    }

    @Test
    void extractApiKey_EmptyHeader_ThrowsUnauthorizedException() {
        // When/Then
        assertThatThrownBy(() -> apiKeyService.extractApiKey(""))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Missing Authorization header");
    }

    @Test
    void extractApiKey_BlankHeader_ThrowsUnauthorizedException() {
        // When/Then
        assertThatThrownBy(() -> apiKeyService.extractApiKey("   "))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Missing Authorization header");
    }

    @Test
    void extractApiKey_InvalidFormat_ThrowsUnauthorizedException() {
        // Given
        String invalidHeader = "Bearer " + plainApiKey;

        // When/Then
        assertThatThrownBy(() -> apiKeyService.extractApiKey(invalidHeader))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid Authorization header format. Expected: ApiKey <key>");
    }

    @Test
    void extractApiKey_MissingKey_ThrowsUnauthorizedException() {
        // Given
        String invalidHeader = "ApiKey ";

        // When/Then
        assertThatThrownBy(() -> apiKeyService.extractApiKey(invalidHeader))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("API key is empty");
    }

    @Test
    void extractApiKey_OnlyPrefix_ThrowsUnauthorizedException() {
        // Given
        String invalidHeader = "ApiKey";

        // When/Then
        assertThatThrownBy(() -> apiKeyService.extractApiKey(invalidHeader))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid Authorization header format. Expected: ApiKey <key>");
    }

    // ==================== authenticateFromHeader Tests ====================

    @Test
    void authenticateFromHeader_ValidHeader_ReturnsMerchant() {
        // Given
        String authHeader = "ApiKey " + plainApiKey;
        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(merchantRepository.findByApiKeyHash(hashedApiKey)).thenReturn(Optional.of(approvedMerchant));

        // When
        Merchant result = apiKeyService.authenticateFromHeader(authHeader);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(MerchantStatus.APPROVED);
    }

    @Test
    void authenticateFromHeader_InvalidHeader_ThrowsUnauthorizedException() {
        // Given
        String invalidHeader = "Bearer token123";

        // When/Then
        assertThatThrownBy(() -> apiKeyService.authenticateFromHeader(invalidHeader))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void authenticateFromHeader_InvalidKey_ThrowsUnauthorizedException() {
        // Given
        String authHeader = "ApiKey " + plainApiKey;
        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(merchantRepository.findByApiKeyHash(hashedApiKey)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> apiKeyService.authenticateFromHeader(authHeader))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or missing API key");
    }
}
