package com.springpay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springpay.dto.request.PaymentCreateRequest;
import com.springpay.dto.request.PaymentStatusUpdateRequest;
import com.springpay.entity.Merchant;
import com.springpay.entity.Payment;
import com.springpay.enums.MerchantStatus;
import com.springpay.enums.PaymentStatus;
import com.springpay.exception.ForbiddenException;
import com.springpay.exception.InvalidStateTransitionException;
import com.springpay.exception.NotFoundException;
import com.springpay.security.ApiKeyAuthenticationToken;
import com.springpay.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PaymentController.
 * Tests REST endpoints with mocked service layer.
 */
@WebMvcTest(value = PaymentController.class,
        excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.springpay\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private Merchant mockMerchant;
    private Payment mockPayment;
    private PaymentCreateRequest validRequest;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        mockMerchant = Merchant.builder()
                .id(1L)
                .name("Test Store")
                .email("merchant@example.com")
                .status(MerchantStatus.APPROVED)
                .build();

        mockPayment = Payment.builder()
                .id(1L)
                .merchant(mockMerchant)
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .description("Test payment")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validRequest = PaymentCreateRequest.builder()
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .description("Test payment")
                .build();

        // Create mock authentication
        mockAuth = new ApiKeyAuthenticationToken("api_key", mockMerchant, Collections.emptyList());
    }

    // ==================== Create Payment Tests ====================

    @Test
    void createPayment_ValidRequest_Returns201() throws Exception {
        // Given
        when(paymentService.createPayment(eq(1L), any(PaymentCreateRequest.class)))
                .thenReturn(mockPayment);

        // When/Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest))
                        .principal(mockAuth))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.merchantId", is(1)))
                .andExpect(jsonPath("$.amount", is(99.99)))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.description", is("Test payment")))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void createPayment_InvalidAmount_Returns400() throws Exception {
        // Given
        PaymentCreateRequest invalidRequest = PaymentCreateRequest.builder()
                .amount(new BigDecimal("-10.00")) // Negative amount
                .currency("USD")
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .principal(mockAuth))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void createPayment_InvalidCurrency_Returns400() throws Exception {
        // Given
        PaymentCreateRequest invalidRequest = PaymentCreateRequest.builder()
                .amount(new BigDecimal("99.99"))
                .currency("INVALID") // Not 3 uppercase letters
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .principal(mockAuth))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void createPayment_MissingRequiredFields_Returns400() throws Exception {
        // Given - missing amount and currency
        PaymentCreateRequest invalidRequest = PaymentCreateRequest.builder()
                .description("Test")
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .principal(mockAuth))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    // ==================== Get Payment Tests ====================

    @Test
    void getPayment_ValidPayment_Returns200() throws Exception {
        // Given
        when(paymentService.getPayment(1L, 1L)).thenReturn(mockPayment);

        // When/Then
        mockMvc.perform(get("/api/v1/payments/1")
                        .principal(mockAuth))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.merchantId", is(1)))
                .andExpect(jsonPath("$.amount", is(99.99)))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void getPayment_NonExistentPayment_Returns404() throws Exception {
        // Given
        when(paymentService.getPayment(999L, 1L))
                .thenThrow(new NotFoundException("Payment not found with ID: 999"));

        // When/Then
        mockMvc.perform(get("/api/v1/payments/999")
                        .principal(mockAuth))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }

    @Test
    void getPayment_DifferentMerchant_Returns403() throws Exception {
        // Given
        when(paymentService.getPayment(1L, 1L))
                .thenThrow(new ForbiddenException("Access denied"));

        // When/Then
        mockMvc.perform(get("/api/v1/payments/1")
                        .principal(mockAuth))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    // ==================== List Payments Tests ====================

    @Test
    void listPayments_Returns200WithPagedResults() throws Exception {
        // Given
        List<Payment> payments = List.of(mockPayment);
        Pageable pageable = PageRequest.of(0, 20);
        Page<Payment> pagedPayments = new PageImpl<>(payments, pageable, payments.size());

        when(paymentService.listPayments(eq(1L), any())).thenReturn(pagedPayments);

        // When/Then
        mockMvc.perform(get("/api/v1/payments")
                        .param("page", "0")
                        .param("size", "20")
                        .principal(mockAuth))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].amount", is(99.99)));
    }

    // ==================== Update Payment Status Tests ====================

    @Test
    void updatePaymentStatus_ValidTransition_Returns200() throws Exception {
        // Given
        PaymentStatusUpdateRequest request = PaymentStatusUpdateRequest.builder()
                .status(PaymentStatus.SUCCESS)
                .build();

        Payment successPayment = Payment.builder()
                .id(1L)
                .merchant(mockMerchant)
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentService.updatePaymentStatus(1L, 1L, PaymentStatus.SUCCESS))
                .thenReturn(successPayment);

        // When/Then
        mockMvc.perform(put("/api/v1/payments/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(mockAuth))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("SUCCESS")));
    }

    @Test
    void updatePaymentStatus_InvalidTransition_Returns400() throws Exception {
        // Given
        PaymentStatusUpdateRequest request = PaymentStatusUpdateRequest.builder()
                .status(PaymentStatus.REFUNDED)
                .build();

        when(paymentService.updatePaymentStatus(1L, 1L, PaymentStatus.REFUNDED))
                .thenThrow(new InvalidStateTransitionException("Cannot transition from PENDING to REFUNDED"));

        // When/Then
        mockMvc.perform(put("/api/v1/payments/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(mockAuth))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("INVALID_STATE_TRANSITION")));
    }

    @Test
    void updatePaymentStatus_MissingStatus_Returns400() throws Exception {
        // Given - missing status field
        String invalidJson = "{}";

        // When/Then
        mockMvc.perform(put("/api/v1/payments/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)
                        .principal(mockAuth))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    // ==================== Refund Payment Tests ====================

    @Test
    void refundPayment_SuccessfulPayment_Returns200() throws Exception {
        // Given
        Payment refundedPayment = Payment.builder()
                .id(1L)
                .merchant(mockMerchant)
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .status(PaymentStatus.REFUNDED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentService.refundPayment(1L, 1L)).thenReturn(refundedPayment);

        // When/Then
        mockMvc.perform(post("/api/v1/payments/1/refund")
                        .principal(mockAuth))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("REFUNDED")));
    }

    @Test
    void refundPayment_PendingPayment_Returns400() throws Exception {
        // Given
        when(paymentService.refundPayment(1L, 1L))
                .thenThrow(new InvalidStateTransitionException("Cannot refund PENDING payment"));

        // When/Then
        mockMvc.perform(post("/api/v1/payments/1/refund")
                        .principal(mockAuth))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("INVALID_STATE_TRANSITION")));
    }

    @Test
    void refundPayment_NonExistentPayment_Returns404() throws Exception {
        // Given
        when(paymentService.refundPayment(999L, 1L))
                .thenThrow(new NotFoundException("Payment not found"));

        // When/Then
        mockMvc.perform(post("/api/v1/payments/999/refund")
                        .principal(mockAuth))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }
}
