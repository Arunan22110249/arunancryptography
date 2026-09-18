package com.orderflow.security;

public record JwtPrincipal(String userId, String role, String tenantId) {
}
