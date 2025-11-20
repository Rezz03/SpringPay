package com.springpay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for merchant action requests (rejection, suspension).
 * Used when an admin needs to provide a reason for the action.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to reject or suspend a merchant account")
public class MerchantActionRequest {

    /**
     * Reason for the action (rejection or suspension).
     */
    @NotBlank(message = "Reason is required")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    @Schema(description = "Reason for rejecting or suspending the merchant", example = "Failed KYC verification due to incomplete documentation", required = true)
    private String reason;
}
