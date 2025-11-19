package com.springpay.enums;

/**
 * Enum representing types of actions that can be performed on a payment.
 * Used for audit trail in the transactions table.
 */
public enum TransactionAction {
    /**
     * Payment was initially created
     */
    CREATE,

    /**
     * Payment status was updated
     */
    STATUS_UPDATE,

    /**
     * Refund was issued for the payment
     */
    REFUND
}