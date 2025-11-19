package com.springpay.controller;

import com.springpay.dto.request.MerchantRegistrationRequest;
import com.springpay.dto.response.LoginResponse;
import com.springpay.dto.response.MerchantRegistrationResponse;
import com.springpay.entity.Merchant;
import com.springpay.exception.ErrorResponse;
import com.springpay.security.ApiKeyAuthenticationToken;
import com.springpay.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for merchant operations.
 * Handles merchant registration, profile management, and account operations.
 */
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Merchants", description = "Merchant registration and management endpoints")
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * Registers a new merchant in the payment gateway.
     *
     * This endpoint accepts merchant registration details including name, email, and password.
     * Upon successful registration:
     * - A cryptographically secure API key is generated
     * - Password is hashed using BCrypt (cost factor: 12)
     * - Merchant status is set to PENDING (requires admin approval)
     * - The plain-text API key is returned (ONLY ONCE - merchant must store it securely)
     *
     * @param request the merchant registration request
     * @return ResponseEntity with MerchantRegistrationResponse (201 Created)
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register a new merchant",
        description = "Creates a new merchant account with PENDING status. Returns API key (shown only once)."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Merchant registered successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MerchantRegistrationResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Validation failed - invalid input data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - email already registered",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<MerchantRegistrationResponse> registerMerchant(
            @Valid @RequestBody MerchantRegistrationRequest request) {

        log.info("Received merchant registration request for email: {}", request.getEmail());

        MerchantRegistrationResponse response = merchantService.registerMerchant(request);

        log.info("Merchant registered successfully with ID: {}", response.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Gets the authenticated merchant's profile.
     *
     * This endpoint requires API key authentication.
     * Returns the profile of the merchant associated with the provided API key.
     *
     * @param authentication the authentication object (injected by Spring Security)
     * @return ResponseEntity with merchant profile (200 OK)
     */
    @GetMapping("/profile")
    @Operation(
        summary = "Get merchant profile",
        description = "Returns the authenticated merchant's profile. Requires API key authentication.",
        security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "ApiKey")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Profile retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoginResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing API key",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<LoginResponse> getProfile(Authentication authentication) {
        // Extract merchant from authentication token
        ApiKeyAuthenticationToken authToken = (ApiKeyAuthenticationToken) authentication;
        Merchant merchant = authToken.getMerchant();

        log.info("Profile request for merchant ID: {}", merchant.getId());

        LoginResponse response = LoginResponse.from(merchant);
        return ResponseEntity.ok(response);
    }
}
