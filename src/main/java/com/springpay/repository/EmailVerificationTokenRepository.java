package com.springpay.repository;

import com.springpay.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for EmailVerificationToken entity.
 * Provides database access methods for email verification token operations.
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    /**
     * Finds a verification token by its token string.
     *
     * @param token the token string
     * @return Optional containing the token if found
     */
    Optional<EmailVerificationToken> findByToken(String token);

    /**
     * Finds all tokens for a specific merchant.
     *
     * @param merchantId the merchant ID
     * @return List of verification tokens
     */
    List<EmailVerificationToken> findByMerchantId(Long merchantId);

    /**
     * Deletes all expired tokens.
     *
     * @param now current timestamp
     * @return number of deleted tokens
     */
    long deleteByExpiresAtBefore(LocalDateTime now);

    /**
     * Finds all unused, non-expired tokens for a merchant.
     *
     * @param merchantId the merchant ID
     * @param used usage status
     * @param now current timestamp
     * @return List of valid tokens
     */
    List<EmailVerificationToken> findByMerchantIdAndUsedAndExpiresAtAfter(
            Long merchantId,
            Boolean used,
            LocalDateTime now
    );
}
