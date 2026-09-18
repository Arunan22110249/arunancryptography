package com.orderflow.security;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

public record CurrentUser(UUID userId, UUID tenantId, String role) {
    public static CurrentUser required(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            return new CurrentUser(UUID.fromString(principal.userId()), UUID.fromString(principal.tenantId()), principal.role());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticated identity");
        }
    }

    public boolean canManageCatalog() {
        return "ADMIN".equals(role) || "OPERATOR".equals(role);
    }
}
