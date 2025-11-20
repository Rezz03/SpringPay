package com.springpay.controller;

import com.springpay.dto.request.GenerateApiKeyRequest;
import com.springpay.dto.response.ApiKeyResponse;
import com.springpay.entity.ApiKey;
import com.springpay.entity.Merchant;
import com.springpay.exception.ErrorResponse;
import com.springpay.security.ApiKeyAuthenticationToken;
import com.springpay.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for API key management operations.
 * Provides endpoints for generating, listing, and revoking API keys.
 */
@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "API Key Management", description = "Endpoints for managing merchant API keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * Generate a new API key for the authenticated merchant.
     * The plain-text key is returned only once and must be stored securely.
     *
     * @param request the API key generation request with label
     * @param authentication the authenticated merchant
     * @return 201 Created with the new API key (plain-text shown only once)
     */
    @PostMapping
    @Operation(
            summary = "Generate new API key",
            description = "Generates a new API key for the authenticated merchant. The plain-text key is shown only once.",
            security = @SecurityRequirement(name = "ApiKey")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "API key generated successfully",
                    content = @Content(schema = @Schema(implementation = ApiKeyResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing API key",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<ApiKeyResponse> generateApiKey(
            @Valid @RequestBody GenerateApiKeyRequest request,
            Authentication authentication) {

        // Extract merchant from authentication
        Merchant merchant = ((ApiKeyAuthenticationToken) authentication).getMerchant();

        log.info("Generating new API key for merchant {} with label: {}", merchant.getId(), request.getLabel());

        // Generate new API key
        Object[] result = apiKeyService.generateAdditionalKey(merchant.getId(), request.getLabel());
        ApiKey apiKey = (ApiKey) result[0];
        String plainTextKey = (String) result[1];

        // Return response with plain-text key (only time it's shown)
        ApiKeyResponse response = ApiKeyResponse.fromEntityWithKey(apiKey, plainTextKey);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all API keys for the authenticated merchant.
     *
     * @param authentication the authenticated merchant
     * @return 200 OK with list of API keys (without plain-text keys)
     */
    @GetMapping
    @Operation(
            summary = "List API keys",
            description = "Lists all API keys for the authenticated merchant. Plain-text keys are not included.",
            security = @SecurityRequirement(name = "ApiKey")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "API keys retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiKeyResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing API key",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<ApiKeyResponse>> listApiKeys(Authentication authentication) {

        // Extract merchant from authentication
        Merchant merchant = ((ApiKeyAuthenticationToken) authentication).getMerchant();

        log.info("Listing API keys for merchant {}", merchant.getId());

        // Get all API keys for merchant
        List<ApiKey> apiKeys = apiKeyService.listApiKeys(merchant.getId());

        // Convert to response DTOs (without plain-text keys)
        List<ApiKeyResponse> response = apiKeys.stream()
                .map(ApiKeyResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Revoke (disable) an API key.
     *
     * @param keyId the API key ID to revoke
     * @param authentication the authenticated merchant
     * @return 200 OK with success message
     */
    @DeleteMapping("/{keyId}")
    @Operation(
            summary = "Revoke API key",
            description = "Revokes (disables) an API key. The key will no longer be valid for authentication.",
            security = @SecurityRequirement(name = "ApiKey")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "API key revoked successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing API key",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - API key does not belong to authenticated merchant",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "API key not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<String> revokeApiKey(
            @PathVariable Long keyId,
            Authentication authentication) {

        // Extract merchant from authentication
        Merchant merchant = ((ApiKeyAuthenticationToken) authentication).getMerchant();

        log.info("Revoking API key {} for merchant {}", keyId, merchant.getId());

        // Revoke the API key
        apiKeyService.revokeApiKey(merchant.getId(), keyId);

        return ResponseEntity.ok("API key revoked successfully");
    }
}
