package com.springpay.service;

import com.springpay.entity.Merchant;
import com.springpay.enums.MerchantStatus;
import com.springpay.exception.UnauthorizedException;
import com.springpay.repository.MerchantRepository;
import com.springpay.util.ApiKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service layer for API key operations.
 * Handles API key validation and merchant authentication via API keys.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final MerchantRepository merchantRepository;
    private final ApiKeyGenerator apiKeyGenerator;

    /**
     * Validates an API key and returns the associated merchant.
     *
     * Process:
     * 1. Extracts API key from Authorization header (format: "ApiKey sk_live_...")
     * 2. Hashes the provided API key using SHA-256
     * 3. Looks up merchant by API key hash
     * 4. Validates merchant status is APPROVED
     * 5. Returns authenticated merchant
     *
     * @param apiKey the plain-text API key from Authorization header
     * @return the authenticated merchant
     * @throws UnauthorizedException if API key is invalid, not found, or merchant is not approved
     */
    public Merchant validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("API key validation failed: API key is null or empty");
            throw new UnauthorizedException("Invalid or missing API key");
        }

        // Hash the provided API key
        String apiKeyHash = apiKeyGenerator.hashApiKey(apiKey);

        // Look up merchant by API key hash
        Optional<Merchant> merchantOpt = merchantRepository.findByApiKeyHash(apiKeyHash);

        if (merchantOpt.isEmpty()) {
            log.warn("API key validation failed: No merchant found with provided API key");
            throw new UnauthorizedException("Invalid or missing API key");
        }

        Merchant merchant = merchantOpt.get();

        // Verify merchant status is APPROVED
        if (merchant.getStatus() != MerchantStatus.APPROVED) {
            log.warn("API key validation failed: Merchant {} has status {} (not APPROVED)",
                    merchant.getId(), merchant.getStatus());
            throw new UnauthorizedException("Merchant account is not approved");
        }

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
}
