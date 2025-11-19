package com.springpay.dto.response;

import com.springpay.entity.Merchant;
import com.springpay.enums.MerchantStatus;
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
public class LoginResponse {

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
     * Merchant's current status (PENDING, APPROVED, REJECTED, SUSPENDED).
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