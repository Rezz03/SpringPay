package com.springpay.repository;

import com.springpay.entity.Payment;
import com.springpay.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Payment entity.
 * Provides database access methods for payment operations.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Finds all payments for a specific merchant.
     *
     * @param merchantId the merchant ID
     * @param pageable pagination information
     * @return Page of payments
     */
    Page<Payment> findByMerchantId(Long merchantId, Pageable pageable);

    /**
     * Finds all payments for a merchant with a specific status.
     *
     * @param merchantId the merchant ID
     * @param status the payment status
     * @param pageable pagination information
     * @return Page of payments
     */
    Page<Payment> findByMerchantIdAndStatus(Long merchantId, PaymentStatus status, Pageable pageable);

    /**
     * Finds a payment by ID and merchant ID (for authorization check).
     *
     * @param id the payment ID
     * @param merchantId the merchant ID
     * @return Optional containing the payment if found and belongs to merchant
     */
    Optional<Payment> findByIdAndMerchantId(Long id, Long merchantId);

    /**
     * Finds all payments created between two dates for a merchant.
     *
     * @param merchantId the merchant ID
     * @param startDate start date
     * @param endDate end date
     * @param pageable pagination information
     * @return Page of payments
     */
    Page<Payment> findByMerchantIdAndCreatedAtBetween(
            Long merchantId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Counts payments by status for a merchant.
     *
     * @param merchantId the merchant ID
     * @param status the payment status
     * @return count of payments
     */
    long countByMerchantIdAndStatus(Long merchantId, PaymentStatus status);

    /**
     * Finds all refundable payments (SUCCESS status) for a merchant.
     *
     * @param merchantId the merchant ID
     * @return List of refundable payments
     */
    @Query("SELECT p FROM Payment p WHERE p.merchant.id = :merchantId AND p.status = 'SUCCESS'")
    List<Payment> findRefundablePayments(@Param("merchantId") Long merchantId);
}
