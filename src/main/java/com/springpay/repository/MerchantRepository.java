package com.springpay.repository;

import com.springpay.entity.Merchant;
import com.springpay.enums.MerchantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Merchant entity.
 * Provides database access methods for merchant operations.
 */
@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    /**
     * Finds a merchant by email address.
     *
     * @param email the merchant's email
     * @return Optional containing the merchant if found
     */
    Optional<Merchant> findByEmail(String email);

    /**
     * Finds a merchant by API key hash.
     *
     * @param apiKeyHash the hashed API key
     * @return Optional containing the merchant if found
     */
    Optional<Merchant> findByApiKeyHash(String apiKeyHash);

    /**
     * Checks if a merchant with the given email exists.
     *
     * @param email the email to check
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Finds all merchants with a specific status.
     *
     * @param status the merchant status
     * @return List of merchants with the given status
     */
    List<Merchant> findByStatus(MerchantStatus status);

    /**
     * Finds all pending merchants (awaiting approval).
     *
     * @return List of pending merchants
     */
    @Query("SELECT m FROM Merchant m WHERE m.status = 'PENDING' ORDER BY m.createdAt ASC")
    List<Merchant> findAllPending();

    /**
     * Counts merchants by status.
     *
     * @param status the merchant status
     * @return count of merchants with the given status
     */
    long countByStatus(MerchantStatus status);
}