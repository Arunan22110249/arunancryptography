package com.orderflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true")
public class RateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    private final int authLimit;
    private final int customerLimit;
    private final int adminLimit;
    private final Duration window;

    public RateLimitFilter(StringRedisTemplate redis,
                           @Value("${app.rate-limit.auth-per-minute:20}") int authLimit,
                           @Value("${app.rate-limit.customer-per-minute:120}") int customerLimit,
                           @Value("${app.rate-limit.admin-per-minute:300}") int adminLimit,
                           @Value("${app.rate-limit.window-seconds:60}") long windowSeconds) {
        this.redis = redis;
        this.authLimit = authLimit;
        this.customerLimit = customerLimit;
        this.adminLimit = adminLimit;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/") || path.startsWith("/api/v1/health") || path.startsWith("/api/v1/demo")) {
            chain.doFilter(request, response);
            return;
        }
        String bucket = path.startsWith("/api/v1/auth/") ? "auth" : path.startsWith("/api/v1/admin/") ? "admin" : "customer";
        int limit = switch (bucket) {
            case "auth" -> authLimit;
            case "admin" -> adminLimit;
            default -> customerLimit;
        };
        String key = "rate:" + bucket + ":" + request.getRemoteAddr();
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, window);
            }
            if (count != null && count > limit) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", Long.toString(window.toSeconds()));
                return;
            }
        } catch (RuntimeException ignored) {
            // Redis is not authoritative; fail open for availability when the limiter is unavailable.
        }
        chain.doFilter(request, response);
    }
}
