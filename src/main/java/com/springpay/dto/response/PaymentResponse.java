package com.springpay.dto.response;

import com.springpay.entity.Payment;
import com.springpay.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for payment responses.
 * Returns payment details after creation or retrieval.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    /**
     * Payment's unique identifier.
     */
    private Long id;

    /**
     * Merchant ID who created the payment.
     */
    private Long merchantId;

    /**
     * Payment amount.
     */
    private BigDecimal amount;

    /**
     * Currency code (ISO 4217 format).
     */
    private String currency;

    /**
     * Payment description.
     */
    private String description;

    /**
     * Payment status (PENDING, SUCCESS, FAILED, REFUNDED).
     */
    private PaymentStatus status;

    /**
     * Timestamp when the payment was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the payment was last updated.
     */
    private LocalDateTime updatedAt;

    /**
     * Creates a PaymentResponse from a Payment entity.
     *
     * @param payment the payment entity
     * @return the payment response DTO
     */
    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .merchantId(payment.getMerchant().getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .description(payment.getDescription())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
