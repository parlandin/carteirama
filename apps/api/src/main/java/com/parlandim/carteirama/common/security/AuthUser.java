package com.parlandim.carteirama.common.security;

import com.parlandim.carteirama.common.error.ApiException;
import org.springframework.security.core.AuthenticatedPrincipal;

import java.io.Serializable;
import java.util.UUID;

public record AuthUser(UUID userId, UUID accountId, boolean admin) implements AuthenticatedPrincipal, Serializable {
    @Override
    public String getName() {
        return userId.toString();
    }

    public UUID requireAccountId() {
        if (accountId == null) {
            throw ApiException.forbidden();
        }
        return accountId;
    }
}
