package com.springpay.dto.response;

import com.springpay.entity.ApiKey;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "API Key response")
public class ApiKeyResponse {

    @Schema(description = "API key ID", example = "1")
    private Long id;

    @Schema(description = "Plain-text API key (only shown on creation)", example = "sk_live_a1b2c3d4e5f6...")
    private String apiKey;

    @Schema(description = "Label/name for the API key", example = "Production API Key")
    private String label;

    @Schema(description = "Whether the key has been revoked", example = "false")
    private Boolean revoked;

    @Schema(description = "Last time this key was used", example = "2025-11-20T12:00:00")
    private LocalDateTime lastUsedAt;

    @Schema(description = "Key creation timestamp", example = "2025-11-20T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Warning message for one-time key display")
    private String warning;

    /**
     * Factory method to create response from ApiKey entity for listing (no plain-text key)
     */
    public static ApiKeyResponse fromEntity(ApiKey apiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .apiKey(null) // Never expose the key after creation
                .label(apiKey.getLabel())
                .revoked(apiKey.getRevoked())
                .lastUsedAt(apiKey.getLastUsedAt())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }

    /**
     * Factory method to create response from ApiKey entity with plain-text key (creation only)
     */
    public static ApiKeyResponse fromEntityWithKey(ApiKey apiKey, String plainTextKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .apiKey(plainTextKey)
                .label(apiKey.getLabel())
                .revoked(apiKey.getRevoked())
                .lastUsedAt(apiKey.getLastUsedAt())
                .createdAt(apiKey.getCreatedAt())
                .warning("This key will only be shown once. Please store it securely.")
                .build();
    }
}
