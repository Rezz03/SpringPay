package com.springpay.repository;

import com.springpay.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Transaction entity.
 * Provides database access methods for transaction audit trail operations.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Finds all transactions for a specific payment.
     *
     * @param paymentId the payment ID
     * @return List of transactions ordered by creation time
     */
    List<Transaction> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    /**
     * Finds all transactions for payments owned by a specific merchant.
     *
     * @param merchantId the merchant ID
     * @return List of transactions
     */
    List<Transaction> findByPaymentMerchantIdOrderByCreatedAtDesc(Long merchantId);
}
