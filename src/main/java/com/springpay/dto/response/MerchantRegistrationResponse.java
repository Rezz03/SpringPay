package com.springpay.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.springpay.entity.Merchant;
import com.springpay.enums.MerchantStatus;
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
public class MerchantRegistrationResponse {

    /**
     * Unique merchant identifier.
     */
    private Long id;

    /**
     * Merchant business name.
     */
    private String name;

    /**
     * Merchant email address.
     */
    private String email;

    /**
     * Plain-text API key (shown only once at registration).
     * WARNING: This key will never be shown again. Merchants must store it securely.
     */
    private String apiKey;

    /**
     * Current merchant status (defaults to PENDING).
     */
    private MerchantStatus status;

    /**
     * Timestamp when the merchant was created.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
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