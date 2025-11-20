package com.springpay.service;

import com.springpay.entity.Payment;
import com.springpay.entity.Transaction;
import com.springpay.enums.PaymentStatus;
import com.springpay.enums.TransactionAction;
import com.springpay.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for transaction operations.
 * Handles audit trail logging for all payment actions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;

    /**
     * Logs a transaction for payment creation.
     *
     * @param payment the payment that was created
     * @return the created transaction entity
     */
    @Transactional
    public Transaction logCreate(Payment payment) {
        log.debug("Logging CREATE transaction for payment ID: {}", payment.getId());
        Transaction transaction = Transaction.forCreate(payment);
        return transactionRepository.save(transaction);
    }

    /**
     * Logs a transaction for payment status update.
     *
     * @param payment the payment being updated
     * @param previousStatus the previous payment status
     * @param newStatus the new payment status
     * @return the created transaction entity
     */
    @Transactional
    public Transaction logStatusUpdate(Payment payment, PaymentStatus previousStatus, PaymentStatus newStatus) {
        log.debug("Logging STATUS_UPDATE transaction for payment ID: {} ({} -> {})",
                payment.getId(), previousStatus, newStatus);
        Transaction transaction = Transaction.forStatusUpdate(payment, previousStatus, newStatus);
        return transactionRepository.save(transaction);
    }

    /**
     * Logs a transaction for payment refund.
     *
     * @param payment the payment being refunded
     * @param reason the reason for the refund
     * @return the created transaction entity
     */
    @Transactional
    public Transaction logRefund(Payment payment, String reason) {
        log.debug("Logging REFUND transaction for payment ID: {}", payment.getId());
        Transaction transaction = Transaction.forRefund(payment, reason);
        return transactionRepository.save(transaction);
    }

    /**
     * Generic method to log any transaction action.
     * Delegates to specific methods based on action type.
     *
     * @param payment the payment associated with the transaction
     * @param action the transaction action being performed
     * @return the created transaction entity
     */
    @Transactional
    public Transaction logTransaction(Payment payment, TransactionAction action) {
        return switch (action) {
            case CREATE -> logCreate(payment);
            case REFUND -> logRefund(payment, "Refund requested");
            case STATUS_UPDATE -> logStatusUpdate(payment, null, payment.getStatus());
        };
    }

    /**
     * Retrieves all transactions for a specific payment.
     *
     * @param paymentId the payment ID
     * @return list of transactions ordered by creation time (descending)
     */
    public List<Transaction> getTransactionsForPayment(Long paymentId) {
        return transactionRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId);
    }

    /**
     * Retrieves all transactions for payments owned by a specific merchant.
     *
     * @param merchantId the merchant ID
     * @return list of transactions ordered by creation time (descending)
     */
    public List<Transaction> getTransactionsForMerchant(Long merchantId) {
        return transactionRepository.findByPaymentMerchantIdOrderByCreatedAtDesc(merchantId);
    }
}
