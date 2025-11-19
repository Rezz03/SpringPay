package com.springpay.service;

import com.springpay.dto.request.MerchantRegistrationRequest;
import com.springpay.dto.response.MerchantRegistrationResponse;
import com.springpay.entity.Merchant;
import com.springpay.enums.MerchantStatus;
import com.springpay.exception.ConflictException;
import com.springpay.exception.NotFoundException;
import com.springpay.repository.MerchantRepository;
import com.springpay.util.ApiKeyGenerator;
import com.springpay.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MerchantService.
 * Uses Mockito for mocking dependencies and AssertJ for assertions.
 */
@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private ApiKeyGenerator apiKeyGenerator;

    @InjectMocks
    private MerchantService merchantService;

    private MerchantRegistrationRequest validRequest;
    private Merchant mockMerchant;
    private String plainApiKey;
    private String hashedApiKey;
    private String hashedPassword;

    @BeforeEach
    void setUp() {
        // Prepare test data
        validRequest = MerchantRegistrationRequest.builder()
                .name("Tulip Store")
                .email("merchant@tulipstore.com")
                .password("SecureP@ss123")
                .build();

        plainApiKey = "sk_live_abc123xyz456def789ghi012jkl345mno678pqr901stu234vwx567yza890bcd123";
        hashedApiKey = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        hashedPassword = "$2a$12$KIXn8TvLKGHQvF4kR0xZ1eGqU5Y7bZ1vK9Xb3pQ2wF8zJ5yH6tC7m";

        mockMerchant = Merchant.builder()
                .id(1L)
                .name("Tulip Store")
                .email("merchant@tulipstore.com")
                .passwordHash(hashedPassword)
                .apiKeyHash(hashedApiKey)
                .status(MerchantStatus.PENDING)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void registerMerchant_ValidRequest_ReturnsMerchantResponse() {
        // Given
        when(merchantRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(apiKeyGenerator.generateApiKey()).thenReturn(plainApiKey);
        when(passwordHasher.hashPassword(validRequest.getPassword())).thenReturn(hashedPassword);
        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(merchantRepository.save(any(Merchant.class))).thenReturn(mockMerchant);

        // When
        MerchantRegistrationResponse response = merchantService.registerMerchant(validRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Tulip Store");
        assertThat(response.getEmail()).isEqualTo("merchant@tulipstore.com");
        assertThat(response.getApiKey()).isEqualTo(plainApiKey);
        assertThat(response.getStatus()).isEqualTo(MerchantStatus.PENDING);
        assertThat(response.getCreatedAt()).isNotNull();

        // Verify interactions
        verify(merchantRepository).existsByEmail(validRequest.getEmail());
        verify(apiKeyGenerator).generateApiKey();
        verify(passwordHasher).hashPassword(validRequest.getPassword());
        verify(apiKeyGenerator).hashApiKey(plainApiKey);
        verify(merchantRepository).save(any(Merchant.class));
    }

    @Test
    void registerMerchant_ValidRequest_SavesMerchantWithCorrectData() {
        // Given
        when(merchantRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(apiKeyGenerator.generateApiKey()).thenReturn(plainApiKey);
        when(passwordHasher.hashPassword(validRequest.getPassword())).thenReturn(hashedPassword);
        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(merchantRepository.save(any(Merchant.class))).thenReturn(mockMerchant);

        ArgumentCaptor<Merchant> merchantCaptor = ArgumentCaptor.forClass(Merchant.class);

        // When
        merchantService.registerMerchant(validRequest);

        // Then
        verify(merchantRepository).save(merchantCaptor.capture());
        Merchant capturedMerchant = merchantCaptor.getValue();

        assertThat(capturedMerchant.getName()).isEqualTo("Tulip Store");
        assertThat(capturedMerchant.getEmail()).isEqualTo("merchant@tulipstore.com");
        assertThat(capturedMerchant.getPasswordHash()).isEqualTo(hashedPassword);
        assertThat(capturedMerchant.getApiKeyHash()).isEqualTo(hashedApiKey);
        assertThat(capturedMerchant.getStatus()).isEqualTo(MerchantStatus.PENDING);
        assertThat(capturedMerchant.getEmailVerified()).isFalse();
    }

    @Test
    void registerMerchant_EmailAlreadyExists_ThrowsConflictException() {
        // Given
        when(merchantRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> merchantService.registerMerchant(validRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email already registered");

        // Verify that no further processing occurred
        verify(merchantRepository).existsByEmail(validRequest.getEmail());
        verify(apiKeyGenerator, never()).generateApiKey();
        verify(passwordHasher, never()).hashPassword(anyString());
        verify(merchantRepository, never()).save(any(Merchant.class));
    }

    @Test
    void registerMerchant_DuplicateEmail_DoesNotGenerateApiKey() {
        // Given
        when(merchantRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> merchantService.registerMerchant(validRequest))
                .isInstanceOf(ConflictException.class);

        // Verify API key generation was never called
        verify(apiKeyGenerator, never()).generateApiKey();
    }

    @Test
    void findById_ExistingMerchant_ReturnsMerchant() {
        // Given
        Long merchantId = 1L;
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(mockMerchant));

        // When
        Merchant result = merchantService.findById(merchantId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(merchantId);
        assertThat(result.getEmail()).isEqualTo("merchant@tulipstore.com");
        verify(merchantRepository).findById(merchantId);
    }

    @Test
    void findById_NonExistingMerchant_ThrowsNotFoundException() {
        // Given
        Long merchantId = 999L;
        when(merchantRepository.findById(merchantId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> merchantService.findById(merchantId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Merchant not found with ID: 999");

        verify(merchantRepository).findById(merchantId);
    }

    @Test
    void findByEmail_ExistingMerchant_ReturnsMerchant() {
        // Given
        String email = "merchant@tulipstore.com";
        when(merchantRepository.findByEmail(email)).thenReturn(Optional.of(mockMerchant));

        // When
        Merchant result = merchantService.findByEmail(email);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        verify(merchantRepository).findByEmail(email);
    }

    @Test
    void findByEmail_NonExistingMerchant_ThrowsNotFoundException() {
        // Given
        String email = "nonexistent@example.com";
        when(merchantRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> merchantService.findByEmail(email))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Merchant not found with email: nonexistent@example.com");

        verify(merchantRepository).findByEmail(email);
    }

    @Test
    void registerMerchant_ValidRequest_UsesCorrectPasswordHasher() {
        // Given
        when(merchantRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(apiKeyGenerator.generateApiKey()).thenReturn(plainApiKey);
        when(passwordHasher.hashPassword(validRequest.getPassword())).thenReturn(hashedPassword);
        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(merchantRepository.save(any(Merchant.class))).thenReturn(mockMerchant);

        // When
        merchantService.registerMerchant(validRequest);

        // Then
        verify(passwordHasher).hashPassword("SecureP@ss123");
    }

    @Test
    void registerMerchant_ValidRequest_HashesApiKeyCorrectly() {
        // Given
        when(merchantRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(apiKeyGenerator.generateApiKey()).thenReturn(plainApiKey);
        when(passwordHasher.hashPassword(validRequest.getPassword())).thenReturn(hashedPassword);
        when(apiKeyGenerator.hashApiKey(plainApiKey)).thenReturn(hashedApiKey);
        when(merchantRepository.save(any(Merchant.class))).thenReturn(mockMerchant);

        // When
        merchantService.registerMerchant(validRequest);

        // Then
        verify(apiKeyGenerator).hashApiKey(plainApiKey);
    }
}
