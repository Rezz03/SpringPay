package com.springpay.dto.response;

import com.springpay.entity.Payment;
import com.springpay.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Payment details response")
public class PaymentResponse {

    /**
     * Payment's unique identifier.
     */
    @Schema(description = "Payment's unique identifier", example = "101")
    private Long id;

    /**
     * Merchant ID who created the payment.
     */
    @Schema(description = "Merchant ID who created the payment", example = "1")
    private Long merchantId;

    /**
     * Payment amount.
     */
    @Schema(description = "Payment amount", example = "49.99")
    private BigDecimal amount;

    /**
     * Currency code (ISO 4217 format).
     */
    @Schema(description = "Currency code in ISO 4217 format", example = "USD")
    private String currency;

    /**
     * Payment description.
     */
    @Schema(description = "Payment description", example = "Order #12345 - Premium Subscription")
    private String description;

    /**
     * Payment status (PENDING, SUCCESS, FAILED, REFUNDED).
     */
    @Schema(description = "Payment status", example = "PENDING")
    private PaymentStatus status;

    /**
     * Timestamp when the payment was created.
     */
    @Schema(description = "Payment creation timestamp", example = "2025-11-20T10:00:00")
    private LocalDateTime createdAt;

    /**
     * Timestamp when the payment was last updated.
     */
    @Schema(description = "Payment last update timestamp", example = "2025-11-20T10:05:00")
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
