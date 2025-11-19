package com.springpay.service;

import com.springpay.dto.request.MerchantRegistrationRequest;
import com.springpay.dto.response.MerchantRegistrationResponse;
import com.springpay.entity.Merchant;
import com.springpay.enums.MerchantStatus;
import com.springpay.exception.ConflictException;
import com.springpay.repository.MerchantRepository;
import com.springpay.util.ApiKeyGenerator;
import com.springpay.util.PasswordHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for merchant operations.
 * Handles business logic for merchant registration, approval, and management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final PasswordHasher passwordHasher;
    private final ApiKeyGenerator apiKeyGenerator;

    /**
     * Registers a new merchant in the system.
     *
     * Process:
     * 1. Validates that email is unique
     * 2. Generates cryptographically secure API key
     * 3. Hashes password using BCrypt (cost factor: 12)
     * 4. Hashes API key using SHA-256
     * 5. Creates merchant with PENDING status
     * 6. Returns response with plain-text API key (shown only once)
     *
     * @param request the merchant registration request
     * @return MerchantRegistrationResponse containing merchant details and API key
     * @throws ConflictException if email already exists
     */
    @Transactional
    public MerchantRegistrationResponse registerMerchant(MerchantRegistrationRequest request) {
        log.info("Processing merchant registration for email: {}", request.getEmail());

        // 1. Check if email already exists
        if (merchantRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email already exists: {}", request.getEmail());
            throw new ConflictException("Email already registered");
        }

        // 2. Generate API key (plain-text, shown only once)
        String plainApiKey = apiKeyGenerator.generateApiKey();
        log.debug("Generated API key for merchant: {}", request.getEmail());

        // 3. Hash password using BCrypt
        String passwordHash = passwordHasher.hashPassword(request.getPassword());

        // 4. Hash API key using SHA-256
        String apiKeyHash = apiKeyGenerator.hashApiKey(plainApiKey);

        // 5. Create merchant entity
        Merchant merchant = Merchant.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .apiKeyHash(apiKeyHash)
                .status(MerchantStatus.PENDING)
                .emailVerified(false)
                .build();

        // 6. Save merchant to database
        Merchant savedMerchant = merchantRepository.save(merchant);
        log.info("Merchant registered successfully with ID: {} (status: PENDING)", savedMerchant.getId());

        // 7. Return response with plain-text API key
        return MerchantRegistrationResponse.from(savedMerchant, plainApiKey);
    }

    /**
     * Finds a merchant by ID.
     *
     * @param id the merchant ID
     * @return the merchant entity
     * @throws com.springpay.exception.NotFoundException if merchant not found
     */
    public Merchant findById(Long id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new com.springpay.exception.NotFoundException("Merchant not found with ID: " + id));
    }

    /**
     * Finds a merchant by email.
     *
     * @param email the merchant email
     * @return the merchant entity
     * @throws com.springpay.exception.NotFoundException if merchant not found
     */
    public Merchant findByEmail(String email) {
        return merchantRepository.findByEmail(email)
                .orElseThrow(() -> new com.springpay.exception.NotFoundException("Merchant not found with email: " + email));
    }
}
