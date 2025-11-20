package com.springpay.service;

import com.springpay.entity.ApiKey;
import com.springpay.entity.Merchant;
import com.springpay.enums.MerchantStatus;
import com.springpay.exception.ForbiddenException;
import com.springpay.exception.NotFoundException;
import com.springpay.exception.UnauthorizedException;
import com.springpay.repository.ApiKeyRepository;
import com.springpay.repository.MerchantRepository;
import com.springpay.util.ApiKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApiKeyService.
 * Tests API key validation, generation, revocation, and listing logic.
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

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
        ApiKey apiKey = ApiKey.builder()
                .id(1L)
                .merchant(approvedMerchant)
                .keyHash(hashedApiKey)
                .revoked(false)
                .build();

        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(apiKeyRepository.findByKeyHash(hashedApiKey)).thenReturn(Optional.of(apiKey));

        // When
        Merchant result = apiKeyService.validateApiKey(plainApiKey);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("merchant@tulipstore.com");
        assertThat(result.getStatus()).isEqualTo(MerchantStatus.APPROVED);

        verify(apiKeyGenerator).hashApiKey(plainApiKey);
        verify(apiKeyRepository).findByKeyHash(hashedApiKey);
        verify(apiKeyRepository).save(any(ApiKey.class)); // Verify last used timestamp updated
    }

    @Test
    void validateApiKey_InvalidKey_ThrowsUnauthorizedException() {
        // Given
        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(apiKeyRepository.findByKeyHash(hashedApiKey)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> apiKeyService.validateApiKey(plainApiKey))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid or missing API key");

        verify(apiKeyGenerator).hashApiKey(plainApiKey);
        verify(apiKeyRepository).findByKeyHash(hashedApiKey);
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
        ApiKey apiKey = ApiKey.builder()
                .id(2L)
                .merchant(pendingMerchant)
                .keyHash(hashedApiKey)
                .revoked(false)
                .build();

        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(apiKeyRepository.findByKeyHash(hashedApiKey)).thenReturn(Optional.of(apiKey));

        // When/Then
        assertThatThrownBy(() -> apiKeyService.validateApiKey(plainApiKey))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Merchant account is not approved");

        verify(apiKeyGenerator).hashApiKey(plainApiKey);
        verify(apiKeyRepository).findByKeyHash(hashedApiKey);
    }

    @Test
    void validateApiKey_SuspendedMerchant_ThrowsUnauthorizedException() {
        // Given
        ApiKey apiKey = ApiKey.builder()
                .id(3L)
                .merchant(suspendedMerchant)
                .keyHash(hashedApiKey)
                .revoked(false)
                .build();

        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(apiKeyRepository.findByKeyHash(hashedApiKey)).thenReturn(Optional.of(apiKey));

        // When/Then
        assertThatThrownBy(() -> apiKeyService.validateApiKey(plainApiKey))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Merchant account is not approved");

        verify(apiKeyGenerator).hashApiKey(plainApiKey);
        verify(apiKeyRepository).findByKeyHash(hashedApiKey);
    }

    @Test
    void validateApiKey_RevokedKey_ThrowsUnauthorizedException() {
        // Given
        ApiKey apiKey = ApiKey.builder()
                .id(1L)
                .merchant(approvedMerchant)
                .keyHash(hashedApiKey)
                .revoked(true) // Revoked
                .build();

        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(apiKeyRepository.findByKeyHash(hashedApiKey)).thenReturn(Optional.of(apiKey));

        // When/Then
        assertThatThrownBy(() -> apiKeyService.validateApiKey(plainApiKey))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("API key has been revoked");

        verify(apiKeyGenerator).hashApiKey(plainApiKey);
        verify(apiKeyRepository).findByKeyHash(hashedApiKey);
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
        ApiKey apiKey = ApiKey.builder()
                .id(1L)
                .merchant(approvedMerchant)
                .keyHash(hashedApiKey)
                .revoked(false)
                .build();

        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(apiKeyRepository.findByKeyHash(hashedApiKey)).thenReturn(Optional.of(apiKey));

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
        when(apiKeyRepository.findByKeyHash(hashedApiKey)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> apiKeyService.authenticateFromHeader(authHeader))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid or missing API key");
    }

    // ==================== generateAdditionalKey Tests ====================

    @Test
    void generateAdditionalKey_ApprovedMerchant_ReturnsApiKey() {
        // Given
        Long merchantId = 1L;
        String label = "Production API Key";
        String plainTextKey = "sk_live_generated123";
        String keyHash = "hash_of_generated_key";

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(approvedMerchant));
        when(apiKeyGenerator.generateApiKey()).thenReturn(plainTextKey);
        when(apiKeyGenerator.hashApiKey(plainTextKey)).thenReturn(keyHash);

        ApiKey savedApiKey = ApiKey.builder()
                .id(10L)
                .merchant(approvedMerchant)
                .keyHash(keyHash)
                .label(label)
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(savedApiKey);

        // When
        Object[] result = apiKeyService.generateAdditionalKey(merchantId, label);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result[0]).isInstanceOf(ApiKey.class);
        assertThat(result[1]).isEqualTo(plainTextKey);

        ApiKey apiKey = (ApiKey) result[0];
        assertThat(apiKey.getId()).isEqualTo(10L);
        assertThat(apiKey.getLabel()).isEqualTo(label);
        assertThat(apiKey.getRevoked()).isFalse();

        verify(merchantRepository).findById(merchantId);
        verify(apiKeyGenerator).generateApiKey();
        verify(apiKeyGenerator).hashApiKey(plainTextKey);
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    void generateAdditionalKey_MerchantNotFound_ThrowsNotFoundException() {
        // Given
        Long merchantId = 999L;
        String label = "Production API Key";

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> apiKeyService.generateAdditionalKey(merchantId, label))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Merchant not found");

        verify(merchantRepository).findById(merchantId);
        verify(apiKeyGenerator, never()).generateApiKey();
    }

    @Test
    void generateAdditionalKey_PendingMerchant_ThrowsUnauthorizedException() {
        // Given
        Long merchantId = 2L;
        String label = "Production API Key";

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(pendingMerchant));

        // When/Then
        assertThatThrownBy(() -> apiKeyService.generateAdditionalKey(merchantId, label))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("approved merchants can generate API keys");

        verify(merchantRepository).findById(merchantId);
        verify(apiKeyGenerator, never()).generateApiKey();
    }

    @Test
    void generateAdditionalKey_SuspendedMerchant_ThrowsUnauthorizedException() {
        // Given
        Long merchantId = 3L;
        String label = "Production API Key";

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(suspendedMerchant));

        // When/Then
        assertThatThrownBy(() -> apiKeyService.generateAdditionalKey(merchantId, label))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("approved merchants can generate API keys");

        verify(merchantRepository).findById(merchantId);
        verify(apiKeyGenerator, never()).generateApiKey();
    }

    // ==================== revokeApiKey Tests ====================

    @Test
    void revokeApiKey_ValidKeyAndOwnership_RevokesSuccessfully() {
        // Given
        Long merchantId = 1L;
        Long keyId = 10L;

        ApiKey apiKey = ApiKey.builder()
                .id(keyId)
                .merchant(approvedMerchant)
                .keyHash("some_hash")
                .label("Test Key")
                .revoked(false)
                .build();

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));

        // When
        apiKeyService.revokeApiKey(merchantId, keyId);

        // Then
        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getRevoked()).isTrue();
    }

    @Test
    void revokeApiKey_AlreadyRevoked_IdempotentSuccess() {
        // Given
        Long merchantId = 1L;
        Long keyId = 10L;

        ApiKey apiKey = ApiKey.builder()
                .id(keyId)
                .merchant(approvedMerchant)
                .keyHash("some_hash")
                .label("Test Key")
                .revoked(true) // Already revoked
                .build();

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));

        // When
        apiKeyService.revokeApiKey(merchantId, keyId);

        // Then - should not save again
        verify(apiKeyRepository, never()).save(any(ApiKey.class));
    }

    @Test
    void revokeApiKey_KeyNotFound_ThrowsNotFoundException() {
        // Given
        Long merchantId = 1L;
        Long keyId = 999L;

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> apiKeyService.revokeApiKey(merchantId, keyId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("API key not found");

        verify(apiKeyRepository).findById(keyId);
    }

    @Test
    void revokeApiKey_WrongMerchant_ThrowsForbiddenException() {
        // Given
        Long merchantId = 1L;
        Long keyId = 10L;

        Merchant differentMerchant = Merchant.builder()
                .id(999L)
                .name("Different Merchant")
                .build();

        ApiKey apiKey = ApiKey.builder()
                .id(keyId)
                .merchant(differentMerchant)
                .keyHash("some_hash")
                .label("Test Key")
                .revoked(false)
                .build();

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));

        // When/Then
        assertThatThrownBy(() -> apiKeyService.revokeApiKey(merchantId, keyId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("do not have permission");

        verify(apiKeyRepository).findById(keyId);
        verify(apiKeyRepository, never()).save(any(ApiKey.class));
    }

    // ==================== listApiKeys Tests ====================

    @Test
    void listApiKeys_ReturnsAllKeys() {
        // Given
        Long merchantId = 1L;

        ApiKey key1 = ApiKey.builder()
                .id(1L)
                .merchant(approvedMerchant)
                .label("Production Key")
                .revoked(false)
                .build();

        ApiKey key2 = ApiKey.builder()
                .id(2L)
                .merchant(approvedMerchant)
                .label("Test Key")
                .revoked(true)
                .build();

        List<ApiKey> keys = Arrays.asList(key1, key2);
        when(apiKeyRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)).thenReturn(keys);

        // When
        List<ApiKey> result = apiKeyService.listApiKeys(merchantId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(key1, key2);

        verify(apiKeyRepository).findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    @Test
    void listActiveApiKeys_ReturnsOnlyActiveKeys() {
        // Given
        Long merchantId = 1L;

        ApiKey activeKey = ApiKey.builder()
                .id(1L)
                .merchant(approvedMerchant)
                .label("Production Key")
                .revoked(false)
                .build();

        List<ApiKey> keys = Arrays.asList(activeKey);
        when(apiKeyRepository.findByMerchantIdAndRevoked(merchantId, false)).thenReturn(keys);

        // When
        List<ApiKey> result = apiKeyService.listActiveApiKeys(merchantId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRevoked()).isFalse();

        verify(apiKeyRepository).findByMerchantIdAndRevoked(merchantId, false);
    }
}
