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

    /**
     * Authenticates a merchant using email and password.
     *
     * Process:
     * 1. Finds merchant by email
     * 2. Verifies password against stored BCrypt hash
     * 3. Returns merchant details if authentication succeeds
     *
     * @param email the merchant's email
     * @param password the merchant's plain-text password
     * @return the authenticated merchant entity
     * @throws com.springpay.exception.NotFoundException if merchant not found
     * @throws com.springpay.exception.UnauthorizedException if password is incorrect
     */
    public Merchant login(String email, String password) {
        log.info("Login attempt for email: {}", email);

        // 1. Find merchant by email
        Merchant merchant = merchantRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login failed: Merchant not found with email: {}", email);
                    return new com.springpay.exception.UnauthorizedException("Invalid email or password");
                });

        // 2. Verify password
        boolean passwordMatches = passwordHasher.verifyPassword(password, merchant.getPasswordHash());
        if (!passwordMatches) {
            log.warn("Login failed: Invalid password for email: {}", email);
            throw new com.springpay.exception.UnauthorizedException("Invalid email or password");
        }

        log.info("Login successful for merchant ID: {} (email: {})", merchant.getId(), email);
        return merchant;
    }

    /**
     * Approves a merchant account, changing status from PENDING to APPROVED.
     * Only merchants with PENDING status can be approved.
     *
     * @param merchantId the merchant ID to approve
     * @return the updated merchant entity
     * @throws com.springpay.exception.NotFoundException if merchant not found
     * @throws com.springpay.exception.InvalidStateTransitionException if merchant is not in PENDING status
     */
    @Transactional
    public Merchant approveMerchant(Long merchantId) {
        log.info("Processing merchant approval for ID: {}", merchantId);

        Merchant merchant = findById(merchantId);

        // Validate current status
        if (merchant.getStatus() != MerchantStatus.PENDING) {
            log.warn("Approval failed: Merchant {} has status {} (expected PENDING)",
                    merchantId, merchant.getStatus());
            throw new com.springpay.exception.InvalidStateTransitionException(
                    "Only merchants with PENDING status can be approved. Current status: " + merchant.getStatus());
        }

        // Update status to APPROVED
        merchant.setStatus(MerchantStatus.APPROVED);
        Merchant savedMerchant = merchantRepository.save(merchant);

        log.info("Merchant {} approved successfully", merchantId);
        return savedMerchant;
    }

    /**
     * Rejects a merchant account, changing status from PENDING to REJECTED.
     * Only merchants with PENDING status can be rejected.
     *
     * @param merchantId the merchant ID to reject
     * @param reason the reason for rejection
     * @return the updated merchant entity
     * @throws com.springpay.exception.NotFoundException if merchant not found
     * @throws com.springpay.exception.InvalidStateTransitionException if merchant is not in PENDING status
     */
    @Transactional
    public Merchant rejectMerchant(Long merchantId, String reason) {
        log.info("Processing merchant rejection for ID: {} with reason: {}", merchantId, reason);

        Merchant merchant = findById(merchantId);

        // Validate current status
        if (merchant.getStatus() != MerchantStatus.PENDING) {
            log.warn("Rejection failed: Merchant {} has status {} (expected PENDING)",
                    merchantId, merchant.getStatus());
            throw new com.springpay.exception.InvalidStateTransitionException(
                    "Only merchants with PENDING status can be rejected. Current status: " + merchant.getStatus());
        }

        // Update status to REJECTED
        merchant.setStatus(MerchantStatus.REJECTED);
        Merchant savedMerchant = merchantRepository.save(merchant);

        log.info("Merchant {} rejected successfully. Reason: {}", merchantId, reason);
        return savedMerchant;
    }

    /**
     * Suspends a merchant account, changing status from APPROVED to SUSPENDED.
     * Only merchants with APPROVED status can be suspended.
     *
     * @param merchantId the merchant ID to suspend
     * @param reason the reason for suspension
     * @return the updated merchant entity
     * @throws com.springpay.exception.NotFoundException if merchant not found
     * @throws com.springpay.exception.InvalidStateTransitionException if merchant is not in APPROVED status
     */
    @Transactional
    public Merchant suspendMerchant(Long merchantId, String reason) {
        log.info("Processing merchant suspension for ID: {} with reason: {}", merchantId, reason);

        Merchant merchant = findById(merchantId);

        // Validate current status
        if (merchant.getStatus() != MerchantStatus.APPROVED) {
            log.warn("Suspension failed: Merchant {} has status {} (expected APPROVED)",
                    merchantId, merchant.getStatus());
            throw new com.springpay.exception.InvalidStateTransitionException(
                    "Only merchants with APPROVED status can be suspended. Current status: " + merchant.getStatus());
        }

        // Update status to SUSPENDED
        merchant.setStatus(MerchantStatus.SUSPENDED);
        Merchant savedMerchant = merchantRepository.save(merchant);

        log.info("Merchant {} suspended successfully. Reason: {}", merchantId, reason);
        return savedMerchant;
    }
}
