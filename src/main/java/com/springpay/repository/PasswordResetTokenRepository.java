package com.springpay.repository;

import com.springpay.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PasswordResetToken entity.
 * Provides database access methods for password reset token operations.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Finds a password reset token by its token string.
     *
     * @param token the token string
     * @return Optional containing the token if found
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Finds all tokens for a specific merchant.
     *
     * @param merchantId the merchant ID
     * @return List of password reset tokens
     */
    List<PasswordResetToken> findByMerchantId(Long merchantId);

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
    List<PasswordResetToken> findByMerchantIdAndUsedAndExpiresAtAfter(
            Long merchantId,
            Boolean used,
            LocalDateTime now
    );
}
