package com.springpay.security;

import com.springpay.entity.Merchant;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom authentication token for API key-based authentication.
 * Represents an authenticated merchant using an API key.
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String apiKey;
    private final Merchant merchant;

    /**
     * Creates an unauthenticated token with just the API key.
     * Used before authentication is performed.
     *
     * @param apiKey the API key from the request
     */
    public ApiKeyAuthenticationToken(String apiKey) {
        super(Collections.emptyList());
        this.apiKey = apiKey;
        this.merchant = null;
        setAuthenticated(false);
    }

    /**
     * Creates an authenticated token with the API key and merchant.
     * Used after successful authentication.
     *
     * @param apiKey the API key from the request
     * @param merchant the authenticated merchant
     * @param authorities the granted authorities (permissions)
     */
    public ApiKeyAuthenticationToken(String apiKey, Merchant merchant, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKey = apiKey;
        this.merchant = merchant;
        setAuthenticated(true);
    }

    /**
     * Returns the credentials (API key).
     *
     * @return the API key
     */
    @Override
    public Object getCredentials() {
        return apiKey;
    }

    /**
     * Returns the principal (authenticated merchant).
     *
     * @return the merchant, or null if not authenticated
     */
    @Override
    public Object getPrincipal() {
        return merchant;
    }

    /**
     * Gets the authenticated merchant.
     *
     * @return the merchant, or null if not authenticated
     */
    public Merchant getMerchant() {
        return merchant;
    }

    /**
     * Gets the API key.
     *
     * @return the API key
     */
    public String getApiKey() {
        return apiKey;
    }
}
