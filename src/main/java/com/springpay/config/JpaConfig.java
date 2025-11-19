package com.springpay.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Configuration for SpringPay Payment Gateway.
 *
 * Enables JPA auditing for automatic timestamp management on entities.
 * This allows entities to use @CreatedDate and @LastModifiedDate annotations
 * for automatic population of createdAt and updatedAt fields.
 *
 * Note: Separated from main application class to avoid conflicts with
 * @WebMvcTest slices that don't load the full JPA context.
 *
 * @author Perez
 * @version 1.0
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}