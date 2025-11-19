package com.springpay.repository;

import com.springpay.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ApiKey entity.
 * Provides database access methods for API key operations.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    /**
     * Finds an API key by its hash value.
     *
     * @param keyHash the hashed API key
     * @return Optional containing the API key if found
     */
    Optional<ApiKey> findByKeyHash(String keyHash);

    /**
     * Finds all API keys for a specific merchant.
     *
     * @param merchantId the merchant ID
     * @return List of API keys
     */
    List<ApiKey> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);

    /**
     * Finds all active (non-revoked) API keys for a merchant.
     *
     * @param merchantId the merchant ID
     * @param revoked revocation status
     * @return List of active API keys
     */
    List<ApiKey> findByMerchantIdAndRevoked(Long merchantId, Boolean revoked);

    /**
     * Checks if an API key hash exists.
     *
     * @param keyHash the hashed API key
     * @return true if exists, false otherwise
     */
    boolean existsByKeyHash(String keyHash);
}
