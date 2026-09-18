package com.orderflow.api.dto;

import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(String email, String password) {}

    public record RegisterRequest(String email, String password, String fullName, String organizationName, String organizationSlug) {}

    public record UserResponse(UUID id, UUID organizationId, String email, String fullName, String role) {}

    public record AuthResponse(String token, UserResponse user) {}
}
