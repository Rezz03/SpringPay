package com.springpay.enums;

/**
 * Enum representing the lifecycle status of a payment transaction.
 * Valid transitions:
 * - PENDING → SUCCESS
 * - PENDING → FAILED
 * - SUCCESS → REFUNDED
 * - FAILED (terminal)
 * - REFUNDED (terminal)
 */
public enum PaymentStatus {
    /**
     * Payment created, awaiting completion
     */
    PENDING,

    /**
     * Payment completed successfully
     */
    SUCCESS,

    /**
     * Payment failed during processing
     */
    FAILED,

    /**
     * Previously successful payment has been refunded
     */
    REFUNDED
}