package com.springpay.enums;

/**
 * Enum representing the lifecycle status of a merchant account.
 */
public enum MerchantStatus {
    /**
     * Merchant has registered but awaits admin approval
     */
    PENDING,

    /**
     * Merchant approved and can create payments
     */
    APPROVED,

    /**
     * Merchant registration rejected by admin
     */
    REJECTED,

    /**
     * Merchant account suspended (can be temporary)
     */
    SUSPENDED
}