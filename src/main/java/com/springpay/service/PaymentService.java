package com.springpay.service;

import com.springpay.dto.request.PaymentCreateRequest;
import com.springpay.entity.Merchant;
import com.springpay.entity.Payment;
import com.springpay.enums.PaymentStatus;
import com.springpay.enums.TransactionAction;
import com.springpay.exception.ForbiddenException;
import com.springpay.exception.InvalidStateTransitionException;
import com.springpay.exception.NotFoundException;
import com.springpay.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for payment operations.
 * Handles business logic for payment creation, retrieval, status updates, and refunds.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MerchantService merchantService;
    private final TransactionService transactionService;

    /**
     * Creates a new payment for the specified merchant.
     * Payment is created with PENDING status and an audit trail transaction is logged.
     *
     * @param merchantId the merchant ID creating the payment
     * @param request the payment creation request
     * @return the created payment entity
     * @throws NotFoundException if merchant not found
     */
    @Transactional
    public Payment createPayment(Long merchantId, PaymentCreateRequest request) {
        log.info("Creating payment for merchant ID: {}, amount: {} {}",
                merchantId, request.getAmount(), request.getCurrency());

        // Validate merchant exists
        Merchant merchant = merchantService.findById(merchantId);

        // Create payment entity
        Payment payment = Payment.builder()
                .merchant(merchant)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .status(PaymentStatus.PENDING)
                .build();

        // Save payment
        Payment savedPayment = paymentRepository.save(payment);

        // Log transaction for audit trail
        transactionService.logTransaction(savedPayment, TransactionAction.CREATE);

        log.info("Payment created successfully: ID={}, Status={}", savedPayment.getId(), savedPayment.getStatus());
        return savedPayment;
    }

    /**
     * Retrieves a payment by ID.
     * Validates that the requesting merchant owns the payment.
     *
     * @param paymentId the payment ID
     * @param merchantId the merchant ID requesting the payment
     * @return the payment entity
     * @throws NotFoundException if payment not found
     * @throws ForbiddenException if merchant doesn't own the payment
     */
    public Payment getPayment(Long paymentId, Long merchantId) {
        log.debug("Retrieving payment ID: {} for merchant ID: {}", paymentId, merchantId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found with ID: " + paymentId));

        // Verify ownership
        if (!payment.getMerchant().getId().equals(merchantId)) {
            log.warn("Merchant {} attempted to access payment {} owned by merchant {}",
                    merchantId, paymentId, payment.getMerchant().getId());
            throw new ForbiddenException("Access denied: You do not have permission to access this payment");
        }

        return payment;
    }

    /**
     * Lists all payments for a specific merchant with pagination.
     *
     * @param merchantId the merchant ID
     * @param pageable pagination parameters
     * @return page of payments
     */
    public Page<Payment> listPayments(Long merchantId, Pageable pageable) {
        log.debug("Listing payments for merchant ID: {}", merchantId);
        return paymentRepository.findByMerchantId(merchantId, pageable);
    }

    /**
     * Updates a payment's status.
     * Validates state transitions and logs audit trail.
     *
     * Valid transitions:
     * - PENDING → SUCCESS
     * - PENDING → FAILED
     * - SUCCESS → REFUNDED
     *
     * @param paymentId the payment ID
     * @param merchantId the merchant ID requesting the update
     * @param newStatus the new payment status
     * @return the updated payment entity
     * @throws NotFoundException if payment not found
     * @throws ForbiddenException if merchant doesn't own the payment
     * @throws InvalidStateTransitionException if status transition is invalid
     */
    @Transactional
    public Payment updatePaymentStatus(Long paymentId, Long merchantId, PaymentStatus newStatus) {
        log.info("Updating payment {} status to {} for merchant {}", paymentId, newStatus, merchantId);

        // Get and validate ownership
        Payment payment = getPayment(paymentId, merchantId);

        // Validate status transition
        if (!isValidStatusTransition(payment.getStatus(), newStatus)) {
            log.warn("Invalid status transition: {} → {} for payment {}",
                    payment.getStatus(), newStatus, paymentId);
            throw new InvalidStateTransitionException(
                    String.format("Cannot transition payment from %s to %s", payment.getStatus(), newStatus));
        }

        // Update status
        PaymentStatus oldStatus = payment.getStatus();
        payment.setStatus(newStatus);
        Payment updatedPayment = paymentRepository.save(payment);

        // Log transaction based on new status
        TransactionAction action = getTransactionActionForStatus(newStatus);
        transactionService.logTransaction(updatedPayment, action);

        log.info("Payment {} status updated: {} → {}", paymentId, oldStatus, newStatus);
        return updatedPayment;
    }

    /**
     * Refunds a payment.
     * Payment must be in SUCCESS status to be refunded.
     *
     * @param paymentId the payment ID
     * @param merchantId the merchant ID requesting the refund
     * @return the refunded payment entity
     * @throws NotFoundException if payment not found
     * @throws ForbiddenException if merchant doesn't own the payment
     * @throws InvalidStateTransitionException if payment is not in SUCCESS status
     */
    @Transactional
    public Payment refundPayment(Long paymentId, Long merchantId) {
        log.info("Processing refund for payment {} by merchant {}", paymentId, merchantId);

        return updatePaymentStatus(paymentId, merchantId, PaymentStatus.REFUNDED);
    }

    /**
     * Validates if a status transition is allowed.
     *
     * Valid transitions:
     * - PENDING → SUCCESS
     * - PENDING → FAILED
     * - SUCCESS → REFUNDED
     *
     * @param currentStatus the current payment status
     * @param newStatus the target payment status
     * @return true if transition is valid, false otherwise
     */
    private boolean isValidStatusTransition(PaymentStatus currentStatus, PaymentStatus newStatus) {
        if (currentStatus == newStatus) {
            return false; // No-op transitions are invalid
        }

        return switch (currentStatus) {
            case PENDING -> newStatus == PaymentStatus.SUCCESS || newStatus == PaymentStatus.FAILED;
            case SUCCESS -> newStatus == PaymentStatus.REFUNDED;
            case FAILED, REFUNDED -> false; // Terminal states
        };
    }

    /**
     * Maps payment status to transaction action for audit logging.
     *
     * @param status the payment status
     * @return the corresponding transaction action
     */
    private TransactionAction getTransactionActionForStatus(PaymentStatus status) {
        return switch (status) {
            case SUCCESS -> TransactionAction.STATUS_UPDATE;
            case FAILED -> TransactionAction.STATUS_UPDATE;
            case REFUNDED -> TransactionAction.REFUND;
            default -> TransactionAction.STATUS_UPDATE;
        };
    }
}
