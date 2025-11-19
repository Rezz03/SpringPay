package com.springpay.entity;

import com.springpay.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a payment transaction in the system.
 * Tracks the lifecycle of payments from PENDING through SUCCESS/FAILED to optional REFUNDED.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WebhookLog> webhookLogs = new ArrayList<>();

    /**
     * Helper method to add a transaction audit record.
     */
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setPayment(this);
    }

    /**
     * Helper method to add a webhook log.
     */
    public void addWebhookLog(WebhookLog webhookLog) {
        webhookLogs.add(webhookLog);
        webhookLog.setPayment(this);
    }

    /**
     * Checks if this payment can be refunded.
     */
    public boolean isRefundable() {
        return status == PaymentStatus.SUCCESS;
    }

    /**
     * Checks if this payment is in a terminal state.
     */
    public boolean isTerminal() {
        return status == PaymentStatus.FAILED || status == PaymentStatus.REFUNDED;
    }
}