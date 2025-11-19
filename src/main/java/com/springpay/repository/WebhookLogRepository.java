package com.springpay.repository;

import com.springpay.entity.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for WebhookLog entity.
 * Provides database access methods for webhook logging operations.
 */
@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {

    /**
     * Finds all webhook logs for a specific payment.
     *
     * @param paymentId the payment ID
     * @return List of webhook logs ordered by delivery time
     */
    List<WebhookLog> findByPaymentIdOrderByDeliveredAtDesc(Long paymentId);

    /**
     * Finds all failed webhook deliveries for retry purposes.
     *
     * @return List of failed webhook logs
     */
    List<WebhookLog> findBySuccessFalseOrderByDeliveredAtAsc();

    /**
     * Counts successful webhook deliveries for a payment.
     *
     * @param paymentId the payment ID
     * @return count of successful deliveries
     */
    long countByPaymentIdAndSuccessTrue(Long paymentId);
}
