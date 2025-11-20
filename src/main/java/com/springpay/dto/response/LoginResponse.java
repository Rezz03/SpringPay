package com.springpay.dto.response;

import com.springpay.entity.Merchant;
import com.springpay.enums.MerchantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for merchant login responses.
 * Returns merchant details after successful authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response after successful merchant login")
public class LoginResponse {

    /**
     * Merchant's unique identifier.
     */
    @Schema(description = "Merchant's unique identifier", example = "1")
    private Long id;

    /**
     * Merchant's business name.
     */
    @Schema(description = "Merchant's business name", example = "Acme Store")
    private String name;

    /**
     * Merchant's email address.
     */
    @Schema(description = "Merchant's email address", example = "merchant@acmestore.com")
    private String email;

    /**
     * Merchant's current status (PENDING, APPROVED, REJECTED, SUSPENDED).
     */
    @Schema(description = "Merchant's account status", example = "APPROVED")
    private MerchantStatus status;

    /**
     * Whether the merchant's email has been verified.
     */
    @Schema(description = "Whether the merchant's email has been verified", example = "true")
    private Boolean emailVerified;

    /**
     * Timestamp when the merchant account was created.
     */
    @Schema(description = "Merchant account creation timestamp", example = "2025-11-20T10:00:00")
    private LocalDateTime createdAt;

    /**
     * Creates a LoginResponse from a Merchant entity.
     * Note: API key is NOT included in login response for security reasons.
     *
     * @param merchant the merchant entity
     * @return the login response DTO
     */
    public static LoginResponse from(Merchant merchant) {
        return LoginResponse.builder()
                .id(merchant.getId())
                .name(merchant.getName())
                .email(merchant.getEmail())
                .status(merchant.getStatus())
                .emailVerified(merchant.getEmailVerified())
                .createdAt(merchant.getCreatedAt())
                .build();
    }
}