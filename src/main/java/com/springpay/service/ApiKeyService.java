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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for API key operations.
 * Handles API key validation, generation, revocation, and merchant authentication via API keys.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final MerchantRepository merchantRepository;
    private final ApiKeyGenerator apiKeyGenerator;

    /**
     * Validates an API key and returns the associated merchant.
     *
     * Process:
     * 1. Hashes the provided API key using SHA-256
     * 2. Looks up API key by hash in api_keys table
     * 3. Validates API key is not revoked
     * 4. Validates merchant status is APPROVED
     * 5. Updates last used timestamp
     * 6. Returns authenticated merchant
     *
     * @param apiKey the plain-text API key from Authorization header
     * @return the authenticated merchant
     * @throws UnauthorizedException if API key is invalid, not found, revoked, or merchant is not approved
     */
    @Transactional
    public Merchant validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("API key validation failed: API key is null or empty");
            throw new UnauthorizedException("Invalid or missing API key");
        }

        // Hash the provided API key
        String apiKeyHash = apiKeyGenerator.hashApiKey(apiKey);

        // Look up API key by hash
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyHash(apiKeyHash);

        if (apiKeyOpt.isEmpty()) {
            log.warn("API key validation failed: No API key found with provided hash");
            throw new UnauthorizedException("Invalid or missing API key");
        }

        ApiKey apiKeyEntity = apiKeyOpt.get();

        // Verify API key is not revoked
        if (apiKeyEntity.getRevoked()) {
            log.warn("API key validation failed: API key {} has been revoked", apiKeyEntity.getId());
            throw new UnauthorizedException("API key has been revoked");
        }

        Merchant merchant = apiKeyEntity.getMerchant();

        // Verify merchant status is APPROVED
        if (merchant.getStatus() != MerchantStatus.APPROVED) {
            log.warn("API key validation failed: Merchant {} has status {} (not APPROVED)",
                    merchant.getId(), merchant.getStatus());
            throw new UnauthorizedException("Merchant account is not approved");
        }

        // Update last used timestamp
        apiKeyEntity.updateLastUsed();
        apiKeyRepository.save(apiKeyEntity);

        log.debug("API key validated successfully for merchant ID: {}", merchant.getId());
        return merchant;
    }

    /**
     * Extracts the API key from the Authorization header.
     * Expected format: "ApiKey sk_live_..."
     *
     * @param authorizationHeader the Authorization header value
     * @return the extracted API key (without "ApiKey " prefix)
     * @throws UnauthorizedException if header format is invalid
     */
    public String extractApiKey(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new UnauthorizedException("Missing Authorization header");
        }

        if (!authorizationHeader.startsWith("ApiKey ")) {
            log.warn("Invalid Authorization header format: {}", authorizationHeader);
            throw new UnauthorizedException("Invalid Authorization header format. Expected: ApiKey <key>");
        }

        String apiKey = authorizationHeader.substring(7); // Remove "ApiKey " prefix

        if (apiKey.isBlank()) {
            throw new UnauthorizedException("API key is empty");
        }

        return apiKey;
    }

    /**
     * Validates an Authorization header and returns the authenticated merchant.
     * Convenience method that combines extractApiKey() and validateApiKey().
     *
     * @param authorizationHeader the Authorization header value
     * @return the authenticated merchant
     * @throws UnauthorizedException if authentication fails
     */
    public Merchant authenticateFromHeader(String authorizationHeader) {
        String apiKey = extractApiKey(authorizationHeader);
        return validateApiKey(apiKey);
    }

    /**
     * Generates a new API key for a merchant.
     *
     * Process:
     * 1. Validates merchant exists and is APPROVED
     * 2. Generates a cryptographically secure API key (256-bit)
     * 3. Hashes the key with SHA-256
     * 4. Stores the hash in database
     * 5. Returns the API key entity and plain-text key
     *
     * @param merchantId the merchant ID
     * @param label descriptive label for the API key
     * @return array where [0] is the ApiKey entity and [1] is the plain-text key string
     * @throws NotFoundException if merchant not found
     * @throws UnauthorizedException if merchant is not approved
     */
    @Transactional
    public Object[] generateAdditionalKey(Long merchantId, String label) {
        // Verify merchant exists
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("Merchant not found with ID: " + merchantId));

        // Verify merchant is approved
        if (merchant.getStatus() != MerchantStatus.APPROVED) {
            log.warn("API key generation failed: Merchant {} has status {} (not APPROVED)",
                    merchantId, merchant.getStatus());
            throw new UnauthorizedException("Only approved merchants can generate API keys");
        }

        // Generate new API key
        String plainTextKey = apiKeyGenerator.generateApiKey();
        String keyHash = apiKeyGenerator.hashApiKey(plainTextKey);

        // Create and save API key entity
        ApiKey apiKey = ApiKey.builder()
                .merchant(merchant)
                .keyHash(keyHash)
                .label(label)
                .revoked(false)
                .build();

        apiKey = apiKeyRepository.save(apiKey);

        log.info("Generated new API key with ID {} for merchant {}", apiKey.getId(), merchantId);

        // Return both the entity and the plain-text key (only time it's exposed)
        return new Object[]{apiKey, plainTextKey};
    }

    /**
     * Revokes (disables) an API key.
     *
     * @param merchantId the merchant ID (for ownership verification)
     * @param keyId the API key ID to revoke
     * @throws NotFoundException if API key not found
     * @throws ForbiddenException if API key doesn't belong to the merchant
     */
    @Transactional
    public void revokeApiKey(Long merchantId, Long keyId) {
        // Find the API key
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new NotFoundException("API key not found with ID: " + keyId));

        // Verify ownership
        if (!apiKey.getMerchant().getId().equals(merchantId)) {
            log.warn("API key revocation failed: Key {} does not belong to merchant {}", keyId, merchantId);
            throw new ForbiddenException("You do not have permission to revoke this API key");
        }

        // Check if already revoked
        if (apiKey.getRevoked()) {
            log.info("API key {} is already revoked", keyId);
            return; // Idempotent: already revoked, no error
        }

        // Revoke the key
        apiKey.revoke();
        apiKeyRepository.save(apiKey);

        log.info("Revoked API key {} for merchant {}", keyId, merchantId);
    }

    /**
     * Lists all API keys for a merchant.
     *
     * @param merchantId the merchant ID
     * @return list of API keys (without plain-text keys)
     */
    @Transactional(readOnly = true)
    public List<ApiKey> listApiKeys(Long merchantId) {
        return apiKeyRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    /**
     * Lists only active (non-revoked) API keys for a merchant.
     *
     * @param merchantId the merchant ID
     * @return list of active API keys
     */
    @Transactional(readOnly = true)
    public List<ApiKey> listActiveApiKeys(Long merchantId) {
        return apiKeyRepository.findByMerchantIdAndRevoked(merchantId, false);
    }
}
