package com.springpay.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for payment creation requests.
 * Used when a merchant creates a new payment request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateRequest {

    /**
     * Payment amount (must be positive).
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @DecimalMax(value = "999999.99", message = "Amount must not exceed 999999.99")
    @Digits(integer = 6, fraction = 2, message = "Amount must have at most 6 digits and 2 decimal places")
    private BigDecimal amount;

    /**
     * Currency code (ISO 4217 format, e.g., USD, EUR, GBP).
     */
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code (e.g., USD, EUR)")
    private String currency;

    /**
     * Payment description (optional).
     */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
