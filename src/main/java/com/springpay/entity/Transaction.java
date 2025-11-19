package com.springpay.entity;

import com.springpay.enums.PaymentStatus;
import com.springpay.enums.TransactionAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an audit trail entry for payment lifecycle changes.
 * Records all actions performed on payments for compliance and debugging.
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private PaymentStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20)
    private PaymentStatus newStatus;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Creates a transaction record for payment creation.
     */
    public static Transaction forCreate(Payment payment) {
        return Transaction.builder()
                .payment(payment)
                .action(TransactionAction.CREATE)
                .newStatus(payment.getStatus())
                .notes("Payment created")
                .build();
    }

    /**
     * Creates a transaction record for status update.
     */
    public static Transaction forStatusUpdate(Payment payment, PaymentStatus previousStatus, PaymentStatus newStatus) {
        return Transaction.builder()
                .payment(payment)
                .action(TransactionAction.STATUS_UPDATE)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .notes(String.format("Status changed from %s to %s", previousStatus, newStatus))
                .build();
    }

    /**
     * Creates a transaction record for refund.
     */
    public static Transaction forRefund(Payment payment, String reason) {
        return Transaction.builder()
                .payment(payment)
                .action(TransactionAction.REFUND)
                .previousStatus(PaymentStatus.SUCCESS)
                .newStatus(PaymentStatus.REFUNDED)
                .notes("Refund issued: " + reason)
                .build();
    }
}