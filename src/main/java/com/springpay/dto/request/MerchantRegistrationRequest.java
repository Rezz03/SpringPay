package com.springpay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for merchant registration request.
 * Contains validation constraints for all required fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to register a new merchant account")
public class MerchantRegistrationRequest {

    /**
     * Merchant business name (1-255 characters).
     */
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
    @Schema(description = "Merchant business name", example = "Acme Store", required = true)
    private String name;

    /**
     * Merchant email address (must be valid format).
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "Merchant email address (must be unique)", example = "merchant@acmestore.com", required = true)
    private String email;

    /**
     * Merchant password (min 8 chars, 1 uppercase, 1 number, 1 special char).
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!]).*$",
        message = "Password must contain at least 1 uppercase letter, 1 number, and 1 special character (@#$%^&+=!)"
    )
    @Schema(description = "Merchant password (min 8 chars, 1 uppercase, 1 number, 1 special char)", example = "SecureP@ss123", required = true)
    private String password;
}