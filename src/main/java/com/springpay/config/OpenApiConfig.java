package com.springpay.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration for SpringPay Payment Gateway.
 *
 * Access the Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access the OpenAPI spec at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:SpringPay Payment Gateway}")
    private String applicationName;

    @Bean
    public OpenAPI springPayOpenAPI() {
        // API Key Security Scheme
        SecurityScheme apiKeyScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("Merchant API Key for authentication");

        // Security Requirement
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("API Key Authentication");

        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .components(new Components()
                        .addSecuritySchemes("API Key Authentication", apiKeyScheme))
                .addSecurityItem(securityRequirement);
    }

    /**
     * API metadata and documentation
     */
    private Info apiInfo() {
        return new Info()
                .title(applicationName)
                .description("""
                        # SpringPay Payment Gateway API

                        A modern payment gateway for processing payments, managing merchants, and handling refunds.

                        ## Authentication
                        Most endpoints require API Key authentication. Include your API key in the `X-API-Key` header:
                        ```
                        X-API-Key: your-api-key-here
                        ```

                        ## Getting Started
                        1. Register a merchant account: `POST /api/merchants/register`
                        2. Retrieve your API key from the registration response
                        3. Use the API key to authenticate all subsequent requests

                        ## Payment Lifecycle
                        - **PENDING**: Payment created, awaiting completion
                        - **SUCCESS**: Payment completed successfully
                        - **FAILED**: Payment failed during processing
                        - **REFUNDED**: Previously successful payment has been refunded

                        ## Rate Limiting
                        API requests are rate-limited to prevent abuse. Contact support if you need higher limits.

                        ## Support
                        For API support, contact: support@springpay.com
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("SpringPay Support")
                        .email("support@springpay.com")
                        .url("https://springpay.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    /**
     * Define available servers (dev, staging, prod)
     */
    private List<Server> serverList() {
        Server devServer = new Server()
                .url("http://localhost:8080")
                .description("Development Server");

        Server stagingServer = new Server()
                .url("https://staging.springpay.com")
                .description("Staging Server");

        Server prodServer = new Server()
                .url("https://api.springpay.com")
                .description("Production Server");

        return List.of(devServer, stagingServer, prodServer);
    }
}
