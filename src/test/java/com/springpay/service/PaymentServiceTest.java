package com.springpay.service;

import com.springpay.dto.request.PaymentCreateRequest;
import com.springpay.entity.Merchant;
import com.springpay.entity.Payment;
import com.springpay.enums.MerchantStatus;
import com.springpay.enums.PaymentStatus;
import com.springpay.exception.ForbiddenException;
import com.springpay.exception.InvalidStateTransitionException;
import com.springpay.exception.NotFoundException;
import com.springpay.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService.
 * Tests business logic for payment operations.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MerchantService merchantService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private PaymentService paymentService;

    private Merchant mockMerchant;
    private Payment mockPayment;
    private PaymentCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        mockMerchant = Merchant.builder()
                .id(1L)
                .name("Test Store")
                .email("merchant@example.com")
                .status(MerchantStatus.APPROVED)
                .build();

        validRequest = PaymentCreateRequest.builder()
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .description("Test payment")
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
    }

    // ==================== Create Payment Tests ====================

    @Test
    void createPayment_ValidRequest_CreatesPaymentWithPendingStatus() {
        // Given
        when(merchantService.findById(1L)).thenReturn(mockMerchant);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // When
        Payment result = paymentService.createPayment(1L, validRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(result.getCurrency()).isEqualTo("USD");

        verify(merchantService).findById(1L);
        verify(paymentRepository).save(any(Payment.class));
        verify(transactionService).logTransaction(any(Payment.class), any());
    }

    @Test
    void createPayment_ValidRequest_LogsTransactionForAuditTrail() {
        // Given
        when(merchantService.findById(1L)).thenReturn(mockMerchant);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // When
        paymentService.createPayment(1L, validRequest);

        // Then
        verify(transactionService).logTransaction(eq(mockPayment), any());
    }

    @Test
    void createPayment_NonExistentMerchant_ThrowsNotFoundException() {
        // Given
        when(merchantService.findById(999L)).thenThrow(new NotFoundException("Merchant not found"));

        // When/Then
        assertThatThrownBy(() -> paymentService.createPayment(999L, validRequest))
                .isInstanceOf(NotFoundException.class);

        verify(merchantService).findById(999L);
        verify(paymentRepository, never()).save(any());
        verify(transactionService, never()).logTransaction(any(), any());
    }

    @Test
    void createPayment_ValidRequest_SavesPaymentWithCorrectData() {
        // Given
        when(merchantService.findById(1L)).thenReturn(mockMerchant);
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        // When
        paymentService.createPayment(1L, validRequest);

        // Then
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment capturedPayment = paymentCaptor.getValue();

        assertThat(capturedPayment.getMerchant()).isEqualTo(mockMerchant);
        assertThat(capturedPayment.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(capturedPayment.getCurrency()).isEqualTo("USD");
        assertThat(capturedPayment.getDescription()).isEqualTo("Test payment");
        assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    // ==================== Get Payment Tests ====================

    @Test
    void getPayment_ValidPaymentAndMerchant_ReturnsPayment() {
        // Given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));

        // When
        Payment result = paymentService.getPayment(1L, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(paymentRepository).findById(1L);
    }

    @Test
    void getPayment_NonExistentPayment_ThrowsNotFoundException() {
        // Given
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> paymentService.getPayment(999L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Payment not found with ID: 999");

        verify(paymentRepository).findById(999L);
    }

    @Test
    void getPayment_DifferentMerchant_ThrowsForbiddenException() {
        // Given - payment belongs to merchant 1, but merchant 2 tries to access it
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));

        // When/Then
        assertThatThrownBy(() -> paymentService.getPayment(1L, 2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Access denied");

        verify(paymentRepository).findById(1L);
    }

    // ==================== List Payments Tests ====================

    @Test
    void listPayments_ReturnsPagedPayments() {
        // Given
        List<Payment> payments = List.of(mockPayment);
        Page<Payment> pagedPayments = new PageImpl<>(payments);
        Pageable pageable = PageRequest.of(0, 20);

        when(paymentRepository.findByMerchantId(1L, pageable)).thenReturn(pagedPayments);

        // When
        Page<Payment> result = paymentService.listPayments(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(mockPayment);

        verify(paymentRepository).findByMerchantId(1L, pageable);
    }

    // ==================== Update Payment Status Tests ====================

    @Test
    void updatePaymentStatus_PendingToSuccess_UpdatesStatusAndLogsTransaction() {
        // Given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // When
        Payment result = paymentService.updatePaymentStatus(1L, 1L, PaymentStatus.SUCCESS);

        // Then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository).save(mockPayment);
        verify(transactionService).logTransaction(any(Payment.class), any());
    }

    @Test
    void updatePaymentStatus_PendingToFailed_UpdatesStatusAndLogsTransaction() {
        // Given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // When
        Payment result = paymentService.updatePaymentStatus(1L, 1L, PaymentStatus.FAILED);

        // Then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(mockPayment);
        verify(transactionService).logTransaction(any(Payment.class), any());
    }

    @Test
    void updatePaymentStatus_SuccessToRefunded_UpdatesStatusAndLogsTransaction() {
        // Given
        mockPayment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // When
        Payment result = paymentService.updatePaymentStatus(1L, 1L, PaymentStatus.REFUNDED);

        // Then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepository).save(mockPayment);
        verify(transactionService).logTransaction(any(Payment.class), any());
    }

    @Test
    void updatePaymentStatus_InvalidTransition_ThrowsInvalidStateTransitionException() {
        // Given - trying to go from PENDING to REFUNDED (invalid)
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));

        // When/Then
        assertThatThrownBy(() -> paymentService.updatePaymentStatus(1L, 1L, PaymentStatus.REFUNDED))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition payment from PENDING to REFUNDED");

        verify(paymentRepository).findById(1L);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void updatePaymentStatus_FailedPayment_ThrowsInvalidStateTransitionException() {
        // Given - FAILED is a terminal state
        mockPayment.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));

        // When/Then
        assertThatThrownBy(() -> paymentService.updatePaymentStatus(1L, 1L, PaymentStatus.SUCCESS))
                .isInstanceOf(InvalidStateTransitionException.class);

        verify(paymentRepository).findById(1L);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void updatePaymentStatus_RefundedPayment_ThrowsInvalidStateTransitionException() {
        // Given - REFUNDED is a terminal state
        mockPayment.setStatus(PaymentStatus.REFUNDED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));

        // When/Then
        assertThatThrownBy(() -> paymentService.updatePaymentStatus(1L, 1L, PaymentStatus.PENDING))
                .isInstanceOf(InvalidStateTransitionException.class);

        verify(paymentRepository).findById(1L);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void updatePaymentStatus_SameStatus_ThrowsInvalidStateTransitionException() {
        // Given - trying to update to same status (no-op)
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));

        // When/Then
        assertThatThrownBy(() -> paymentService.updatePaymentStatus(1L, 1L, PaymentStatus.PENDING))
                .isInstanceOf(InvalidStateTransitionException.class);

        verify(paymentRepository).findById(1L);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void updatePaymentStatus_DifferentMerchant_ThrowsForbiddenException() {
        // Given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));

        // When/Then
        assertThatThrownBy(() -> paymentService.updatePaymentStatus(1L, 2L, PaymentStatus.SUCCESS))
                .isInstanceOf(ForbiddenException.class);

        verify(paymentRepository).findById(1L);
        verify(paymentRepository, never()).save(any());
    }

    // ==================== Refund Payment Tests ====================

    @Test
    void refundPayment_SuccessfulPayment_RefundsPayment() {
        // Given
        mockPayment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // When
        Payment result = paymentService.refundPayment(1L, 1L);

        // Then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepository).save(mockPayment);
        verify(transactionService).logTransaction(any(Payment.class), any());
    }

    @Test
    void refundPayment_PendingPayment_ThrowsInvalidStateTransitionException() {
        // Given - can't refund a PENDING payment
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));

        // When/Then
        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L))
                .isInstanceOf(InvalidStateTransitionException.class);

        verify(paymentRepository).findById(1L);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void refundPayment_FailedPayment_ThrowsInvalidStateTransitionException() {
        // Given - can't refund a FAILED payment
        mockPayment.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));

        // When/Then
        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L))
                .isInstanceOf(InvalidStateTransitionException.class);

        verify(paymentRepository).findById(1L);
        verify(paymentRepository, never()).save(any());
    }
}
