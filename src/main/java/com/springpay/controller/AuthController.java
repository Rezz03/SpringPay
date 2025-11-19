package com.springpay.controller;

import com.springpay.dto.request.LoginRequest;
import com.springpay.dto.response.LoginResponse;
import com.springpay.entity.Merchant;
import com.springpay.exception.ErrorResponse;
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
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * Handles merchant login and authentication-related endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and login endpoints")
public class AuthController {

    private final MerchantService merchantService;

    /**
     * Authenticates a merchant using email and password.
     *
     * This endpoint verifies the merchant's credentials and returns merchant details if successful.
     * The password is verified against the stored BCrypt hash.
     *
     * Security Notes:
     * - Returns generic "Invalid email or password" message to prevent email enumeration
     * - Does NOT return API key (use registration response for that)
     * - Password is never logged or returned in response
     *
     * @param request the login request containing email and password
     * @return ResponseEntity with LoginResponse (200 OK)
     */
    @PostMapping("/login")
    @Operation(
        summary = "Merchant login",
        description = "Authenticates a merchant using email and password. Returns merchant details on success."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoginResponse.class)
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
            responseCode = "401",
            description = "Unauthorized - invalid email or password",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());

        Merchant merchant = merchantService.login(request.getEmail(), request.getPassword());
        LoginResponse response = LoginResponse.from(merchant);

        log.info("Login successful for merchant ID: {}", merchant.getId());

        return ResponseEntity.ok(response);
    }
}
