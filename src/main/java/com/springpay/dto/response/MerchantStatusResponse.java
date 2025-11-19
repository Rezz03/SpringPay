package com.springpay.dto.response;

import com.springpay.entity.Merchant;
import com.springpay.enums.MerchantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for merchant status responses.
 * Returned after approval, rejection, or suspension operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantStatusResponse {

    /**
     * Merchant's unique identifier.
     */
    private Long id;

    /**
     * Merchant's business name.
     */
    private String name;

    /**
     * Merchant's email address.
     */
    private String email;

    /**
     * Merchant's current status.
     */
    private MerchantStatus status;

    /**
     * Whether the merchant's email has been verified.
     */
    private Boolean emailVerified;

    /**
     * Timestamp when the merchant account was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the merchant account was last updated.
     */
    private LocalDateTime updatedAt;

    /**
     * Creates a MerchantStatusResponse from a Merchant entity.
     *
     * @param merchant the merchant entity
     * @return the merchant status response DTO
     */
    public static MerchantStatusResponse from(Merchant merchant) {
        return MerchantStatusResponse.builder()
                .id(merchant.getId())
                .name(merchant.getName())
                .email(merchant.getEmail())
                .status(merchant.getStatus())
                .emailVerified(merchant.getEmailVerified())
                .createdAt(merchant.getCreatedAt())
                .updatedAt(merchant.getUpdatedAt())
                .build();
    }
}
