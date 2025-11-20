package com.springpay.dto.request;

import com.springpay.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to update a payment's status")
public class PaymentStatusUpdateRequest {

    /**
     * New payment status.
     */
    @NotNull(message = "Status is required")
    @Schema(description = "New payment status (valid transitions: PENDING→SUCCESS, PENDING→FAILED, SUCCESS→REFUNDED)", example = "SUCCESS", required = true)
    private PaymentStatus status;
}
