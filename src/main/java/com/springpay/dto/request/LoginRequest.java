package com.springpay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for merchant login requests.
 * Used for email/password authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to authenticate a merchant with email and password")
public class LoginRequest {

    /**
     * Merchant's email address (used as username).
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "Merchant's email address", example = "merchant@acmestore.com", required = true)
    private String email;

    /**
     * Merchant's password (plain-text, will be verified against BCrypt hash).
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Schema(description = "Merchant's password", example = "SecureP@ss123", required = true)
    private String password;
}