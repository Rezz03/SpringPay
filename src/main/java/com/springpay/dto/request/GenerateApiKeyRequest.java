package com.springpay.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to generate a new API key")
public class GenerateApiKeyRequest {

    @NotBlank(message = "Label is required")
    @Size(min = 1, max = 100, message = "Label must be between 1 and 100 characters")
    @Schema(description = "Label/name for the API key", example = "Production API Key", required = true)
    private String label;
}
