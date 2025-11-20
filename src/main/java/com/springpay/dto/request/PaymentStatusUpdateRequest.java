package com.springpay.dto.request;

import com.springpay.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for payment status update requests.
 * Used when updating a payment's status (e.g., marking as SUCCESS or FAILED).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusUpdateRequest {

    /**
     * New payment status.
     */
    @NotNull(message = "Status is required")
    private PaymentStatus status;
}
