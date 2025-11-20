package com.springpay.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.springpay.entity.Merchant;
import com.springpay.enums.MerchantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for merchant registration response.
 * Contains merchant details and the plain-text API key (shown only once).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response after successful merchant registration")
public class MerchantRegistrationResponse {

    /**
     * Unique merchant identifier.
     */
    @Schema(description = "Unique merchant identifier", example = "1")
    private Long id;

    /**
     * Merchant business name.
     */
    @Schema(description = "Merchant business name", example = "Acme Store")
    private String name;

    /**
     * Merchant email address.
     */
    @Schema(description = "Merchant email address", example = "merchant@acmestore.com")
    private String email;

    /**
     * Plain-text API key (shown only once at registration).
     * WARNING: This key will never be shown again. Merchants must store it securely.
     */
    @Schema(description = "Plain-text API key (shown only once - store securely!)", example = "sk_live_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6")
    private String apiKey;

    /**
     * Current merchant status (defaults to PENDING).
     */
    @Schema(description = "Merchant account status", example = "PENDING")
    private MerchantStatus status;

    /**
     * Timestamp when the merchant was created.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "Merchant account creation timestamp", example = "2025-11-20T10:00:00Z")
    private LocalDateTime createdAt;

    /**
     * Factory method to create response from Merchant entity and plain-text API key.
     *
     * @param merchant the merchant entity
     * @param plainApiKey the plain-text API key (not hashed)
     * @return MerchantRegistrationResponse instance
     */
    public static MerchantRegistrationResponse from(Merchant merchant, String plainApiKey) {
        return MerchantRegistrationResponse.builder()
                .id(merchant.getId())
                .name(merchant.getName())
                .email(merchant.getEmail())
                .apiKey(plainApiKey)
                .status(merchant.getStatus())
                .createdAt(merchant.getCreatedAt())
                .build();
    }
}