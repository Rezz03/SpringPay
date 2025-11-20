package com.springpay.controller;

import com.springpay.dto.request.PaymentCreateRequest;
import com.springpay.dto.request.PaymentStatusUpdateRequest;
import com.springpay.dto.response.PaymentResponse;
import com.springpay.entity.Merchant;
import com.springpay.entity.Payment;
import com.springpay.exception.ErrorResponse;
import com.springpay.security.ApiKeyAuthenticationToken;
import com.springpay.service.PaymentService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for payment operations.
 * Handles payment creation, retrieval, status updates, and refunds.
 * All endpoints require API key authentication.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment creation and management endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Creates a new payment for the authenticated merchant.
     *
     * @param authentication the authentication object (contains merchant)
     * @param request the payment creation request
     * @return ResponseEntity with PaymentResponse (201 Created)
     */
    @PostMapping
    @Operation(
        summary = "Create a new payment",
        description = "Creates a payment request for the authenticated merchant. " +
                     "Payment is created with PENDING status.",
        security = @SecurityRequirement(name = "ApiKey")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Payment created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponse.class)
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
            description = "Unauthorized - invalid or missing API key",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<PaymentResponse> createPayment(
            Authentication authentication,
            @Valid @RequestBody PaymentCreateRequest request) {

        ApiKeyAuthenticationToken authToken = (ApiKeyAuthenticationToken) authentication;
        Merchant merchant = authToken.getMerchant();

        log.info("Payment creation request from merchant ID: {}", merchant.getId());

        Payment payment = paymentService.createPayment(merchant.getId(), request);
        PaymentResponse response = PaymentResponse.from(payment);

        log.info("Payment created: ID={}", payment.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a payment by ID.
     *
     * @param paymentId the payment ID
     * @param authentication the authentication object (contains merchant)
     * @return ResponseEntity with PaymentResponse (200 OK)
     */
    @GetMapping("/{paymentId}")
    @Operation(
        summary = "Get payment by ID",
        description = "Retrieves a payment by ID. Merchant can only access their own payments.",
        security = @SecurityRequirement(name = "ApiKey")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Payment retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing API key",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - payment belongs to another merchant",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable Long paymentId,
            Authentication authentication) {

        ApiKeyAuthenticationToken authToken = (ApiKeyAuthenticationToken) authentication;
        Merchant merchant = authToken.getMerchant();

        log.info("Payment retrieval request: Payment ID={}, Merchant ID={}", paymentId, merchant.getId());

        Payment payment = paymentService.getPayment(paymentId, merchant.getId());
        PaymentResponse response = PaymentResponse.from(payment);

        return ResponseEntity.ok(response);
    }

    /**
     * Lists all payments for the authenticated merchant with pagination.
     *
     * @param authentication the authentication object (contains merchant)
     * @param page page number (default: 0)
     * @param size page size (default: 20)
     * @return ResponseEntity with Page of PaymentResponse (200 OK)
     */
    @GetMapping
    @Operation(
        summary = "List payments",
        description = "Lists all payments for the authenticated merchant with pagination. " +
                     "Results are sorted by creation date (newest first).",
        security = @SecurityRequirement(name = "ApiKey")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Payments retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
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
    public ResponseEntity<Page<PaymentResponse>> listPayments(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ApiKeyAuthenticationToken authToken = (ApiKeyAuthenticationToken) authentication;
        Merchant merchant = authToken.getMerchant();

        log.info("Payment list request from merchant ID: {} (page={}, size={})", merchant.getId(), page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Payment> payments = paymentService.listPayments(merchant.getId(), pageable);
        Page<PaymentResponse> response = payments.map(PaymentResponse::from);

        return ResponseEntity.ok(response);
    }

    /**
     * Updates a payment's status.
     *
     * @param paymentId the payment ID
     * @param authentication the authentication object (contains merchant)
     * @param request the status update request
     * @return ResponseEntity with PaymentResponse (200 OK)
     */
    @PutMapping("/{paymentId}/status")
    @Operation(
        summary = "Update payment status",
        description = "Updates a payment's status. Valid transitions: PENDING→SUCCESS, PENDING→FAILED, SUCCESS→REFUNDED.",
        security = @SecurityRequirement(name = "ApiKey")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Payment status updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid state transition",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing API key",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - payment belongs to another merchant",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<PaymentResponse> updatePaymentStatus(
            @PathVariable Long paymentId,
            Authentication authentication,
            @Valid @RequestBody PaymentStatusUpdateRequest request) {

        ApiKeyAuthenticationToken authToken = (ApiKeyAuthenticationToken) authentication;
        Merchant merchant = authToken.getMerchant();

        log.info("Payment status update request: Payment ID={}, Merchant ID={}, New Status={}",
                paymentId, merchant.getId(), request.getStatus());

        Payment payment = paymentService.updatePaymentStatus(paymentId, merchant.getId(), request.getStatus());
        PaymentResponse response = PaymentResponse.from(payment);

        return ResponseEntity.ok(response);
    }

    /**
     * Refunds a payment.
     *
     * @param paymentId the payment ID
     * @param authentication the authentication object (contains merchant)
     * @return ResponseEntity with PaymentResponse (200 OK)
     */
    @PostMapping("/{paymentId}/refund")
    @Operation(
        summary = "Refund payment",
        description = "Refunds a payment. Payment must be in SUCCESS status to be refunded.",
        security = @SecurityRequirement(name = "ApiKey")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Payment refunded successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid state transition - payment is not in SUCCESS status",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing API key",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - payment belongs to another merchant",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable Long paymentId,
            Authentication authentication) {

        ApiKeyAuthenticationToken authToken = (ApiKeyAuthenticationToken) authentication;
        Merchant merchant = authToken.getMerchant();

        log.info("Payment refund request: Payment ID={}, Merchant ID={}", paymentId, merchant.getId());

        Payment payment = paymentService.refundPayment(paymentId, merchant.getId());
        PaymentResponse response = PaymentResponse.from(payment);

        return ResponseEntity.ok(response);
    }
}
