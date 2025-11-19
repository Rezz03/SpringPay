package com.springpay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springpay.dto.request.MerchantActionRequest;
import com.springpay.entity.Merchant;
import com.springpay.enums.MerchantStatus;
import com.springpay.exception.InvalidStateTransitionException;
import com.springpay.exception.NotFoundException;
import com.springpay.service.MerchantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminController.
 * Tests admin endpoints for merchant approval, rejection, and suspension.
 */
@WebMvcTest(value = AdminController.class,
        excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.springpay\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MerchantService merchantService;

    private Merchant pendingMerchant;
    private Merchant approvedMerchant;
    private Merchant suspendedMerchant;

    @BeforeEach
    void setUp() {
        pendingMerchant = Merchant.builder()
                .id(1L)
                .name("Pending Store")
                .email("pending@example.com")
                .status(MerchantStatus.PENDING)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        approvedMerchant = Merchant.builder()
                .id(1L)
                .name("Pending Store")
                .email("pending@example.com")
                .status(MerchantStatus.APPROVED)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        suspendedMerchant = Merchant.builder()
                .id(2L)
                .name("Suspended Store")
                .email("suspended@example.com")
                .status(MerchantStatus.SUSPENDED)
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== Approve Merchant Tests ====================

    @Test
    void approveMerchant_ValidMerchant_Returns200() throws Exception {
        // Given
        when(merchantService.approveMerchant(1L)).thenReturn(approvedMerchant);

        // When/Then
        mockMvc.perform(post("/api/v1/admin/merchants/1/approve"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Pending Store")))
                .andExpect(jsonPath("$.email", is("pending@example.com")))
                .andExpect(jsonPath("$.status", is("APPROVED")));
    }

    @Test
    void approveMerchant_NonExistentMerchant_Returns404() throws Exception {
        // Given
        when(merchantService.approveMerchant(999L))
                .thenThrow(new NotFoundException("Merchant not found with ID: 999"));

        // When/Then
        mockMvc.perform(post("/api/v1/admin/merchants/999/approve"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Merchant not found with ID: 999")));
    }

    @Test
    void approveMerchant_AlreadyApprovedMerchant_Returns400() throws Exception {
        // Given
        when(merchantService.approveMerchant(1L))
                .thenThrow(new InvalidStateTransitionException(
                        "Only merchants with PENDING status can be approved. Current status: APPROVED"));

        // When/Then
        mockMvc.perform(post("/api/v1/admin/merchants/1/approve"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("INVALID_STATE_TRANSITION")));
    }

    // ==================== Reject Merchant Tests ====================

    @Test
    void rejectMerchant_ValidRequest_Returns200() throws Exception {
        // Given
        MerchantActionRequest request = MerchantActionRequest.builder()
                .reason("Failed background check")
                .build();

        Merchant rejectedMerchant = Merchant.builder()
                .id(1L)
                .name("Pending Store")
                .email("pending@example.com")
                .status(MerchantStatus.REJECTED)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(merchantService.rejectMerchant(1L, "Failed background check"))
                .thenReturn(rejectedMerchant);

        // When/Then
        mockMvc.perform(post("/api/v1/admin/merchants/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("REJECTED")));
    }

    @Test
    void rejectMerchant_MissingReason_Returns400() throws Exception {
        // Given
        MerchantActionRequest request = MerchantActionRequest.builder()
                .reason(null)
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/admin/merchants/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void rejectMerchant_ReasonTooShort_Returns400() throws Exception {
        // Given
        MerchantActionRequest request = MerchantActionRequest.builder()
                .reason("Short")
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/admin/merchants/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void rejectMerchant_NonExistentMerchant_Returns404() throws Exception {
        // Given
        MerchantActionRequest request = MerchantActionRequest.builder()
                .reason("Failed background check")
                .build();

        when(merchantService.rejectMerchant(anyLong(), anyString()))
                .thenThrow(new NotFoundException("Merchant not found"));

        // When/Then
        mockMvc.perform(post("/api/v1/admin/merchants/999/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ==================== Suspend Merchant Tests ====================

    @Test
    void suspendMerchant_ValidRequest_Returns200() throws Exception {
        // Given
        MerchantActionRequest request = MerchantActionRequest.builder()
                .reason("Fraudulent activity detected")
                .build();

        when(merchantService.suspendMerchant(2L, "Fraudulent activity detected"))
                .thenReturn(suspendedMerchant);

        // When/Then
        mockMvc.perform(post("/api/v1/admin/merchants/2/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.status", is("SUSPENDED")));
    }

    @Test
    void suspendMerchant_MissingReason_Returns400() throws Exception {
        // Given
        MerchantActionRequest request = MerchantActionRequest.builder()
                .reason("")
                .build();

        // When/Then
        mockMvc.perform(post("/api/v1/admin/merchants/2/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void suspendMerchant_PendingMerchant_Returns400() throws Exception {
        // Given
        MerchantActionRequest request = MerchantActionRequest.builder()
                .reason("Fraudulent activity detected")
                .build();

        when(merchantService.suspendMerchant(anyLong(), anyString()))
                .thenThrow(new InvalidStateTransitionException(
                        "Only merchants with APPROVED status can be suspended"));

        // When/Then
        mockMvc.perform(post("/api/v1/admin/merchants/1/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("INVALID_STATE_TRANSITION")));
    }

    @Test
    void suspendMerchant_EmptyRequestBody_Returns400() throws Exception {
        // When/Then
        mockMvc.perform(post("/api/v1/admin/merchants/1/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void suspendMerchant_MalformedJson_Returns400() throws Exception {
        // When/Then
        mockMvc.perform(post("/api/v1/admin/merchants/1/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("MALFORMED_REQUEST")));
    }
}
