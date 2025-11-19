package com.springpay.controller;

import com.springpay.dto.request.MerchantActionRequest;
import com.springpay.dto.response.MerchantStatusResponse;
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
 * REST controller for admin operations.
 * Handles merchant approval, rejection, and suspension workflows.
 *
 * NOTE: In a production system, these endpoints would require admin-level authentication.
 * For this MVP, we're implementing the business logic without role-based access control.
 */
@RestController
@RequestMapping("/api/v1/admin/merchants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Admin endpoints for merchant management")
public class AdminController {

    private final MerchantService merchantService;

    /**
     * Approves a merchant account.
     * Changes merchant status from PENDING to APPROVED.
     *
     * @param merchantId the merchant ID to approve
     * @return ResponseEntity with updated merchant status (200 OK)
     */
    @PostMapping("/{merchantId}/approve")
    @Operation(
        summary = "Approve merchant account",
        description = "Approves a merchant account, changing status from PENDING to APPROVED. " +
                     "Only merchants with PENDING status can be approved."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Merchant approved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MerchantStatusResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid state transition - merchant is not in PENDING status",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Merchant not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<MerchantStatusResponse> approveMerchant(@PathVariable Long merchantId) {
        log.info("Admin approval request for merchant ID: {}", merchantId);

        Merchant merchant = merchantService.approveMerchant(merchantId);
        MerchantStatusResponse response = MerchantStatusResponse.from(merchant);

        log.info("Merchant {} approved successfully by admin", merchantId);

        return ResponseEntity.ok(response);
    }

    /**
     * Rejects a merchant account.
     * Changes merchant status from PENDING to REJECTED.
     *
     * @param merchantId the merchant ID to reject
     * @param request the rejection request containing the reason
     * @return ResponseEntity with updated merchant status (200 OK)
     */
    @PostMapping("/{merchantId}/reject")
    @Operation(
        summary = "Reject merchant account",
        description = "Rejects a merchant account, changing status from PENDING to REJECTED. " +
                     "Only merchants with PENDING status can be rejected. Requires a reason."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Merchant rejected successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MerchantStatusResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid state transition or validation failure",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Merchant not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<MerchantStatusResponse> rejectMerchant(
            @PathVariable Long merchantId,
            @Valid @RequestBody MerchantActionRequest request) {

        log.info("Admin rejection request for merchant ID: {} with reason: {}", merchantId, request.getReason());

        Merchant merchant = merchantService.rejectMerchant(merchantId, request.getReason());
        MerchantStatusResponse response = MerchantStatusResponse.from(merchant);

        log.info("Merchant {} rejected successfully by admin", merchantId);

        return ResponseEntity.ok(response);
    }

    /**
     * Suspends a merchant account.
     * Changes merchant status from APPROVED to SUSPENDED.
     *
     * @param merchantId the merchant ID to suspend
     * @param request the suspension request containing the reason
     * @return ResponseEntity with updated merchant status (200 OK)
     */
    @PostMapping("/{merchantId}/suspend")
    @Operation(
        summary = "Suspend merchant account",
        description = "Suspends a merchant account, changing status from APPROVED to SUSPENDED. " +
                     "Only merchants with APPROVED status can be suspended. Requires a reason."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Merchant suspended successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MerchantStatusResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid state transition or validation failure",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Merchant not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<MerchantStatusResponse> suspendMerchant(
            @PathVariable Long merchantId,
            @Valid @RequestBody MerchantActionRequest request) {

        log.info("Admin suspension request for merchant ID: {} with reason: {}", merchantId, request.getReason());

        Merchant merchant = merchantService.suspendMerchant(merchantId, request.getReason());
        MerchantStatusResponse response = MerchantStatusResponse.from(merchant);

        log.info("Merchant {} suspended successfully by admin", merchantId);

        return ResponseEntity.ok(response);
    }
}
