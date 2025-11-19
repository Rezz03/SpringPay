package com.springpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SpringPay Payment Gateway Application
 *
 * Main entry point for the Spring Boot application.
 * Handles merchant registration, payment lifecycle management,
 * and webhook notifications.
 *
 * Note: JPA auditing is configured in JpaConfig.java to avoid
 * conflicts with web slice tests (@WebMvcTest).
 *
 * @author Perez
 * @version 1.0
 */
@SpringBootApplication
public class SpringPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringPayApplication.class, args);
    }
}
