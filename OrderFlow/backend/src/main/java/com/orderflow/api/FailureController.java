package com.orderflow.api;

import com.orderflow.failure.FailureModeService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.orderflow.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/admin/failures")
public class FailureController {
    private final FailureModeService failureModeService;

    public FailureController(FailureModeService failureModeService) {
        this.failureModeService = failureModeService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(Authentication authentication) {
        requireOperator(authentication);
        return ResponseEntity.ok(Map.of("enabled", failureModeService.isEnabled(), "modes", failureModeService.all()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> set(@RequestBody FailureRequest request, Authentication authentication) {
        requireOperator(authentication);
        failureModeService.set(request.type(), request.active());
        return ResponseEntity.ok(Map.of("type", request.type(), "active", request.active()));
    }

    @DeleteMapping("/{type}")
    public ResponseEntity<Void> clear(@PathVariable String type, Authentication authentication) {
        requireOperator(authentication);
        failureModeService.clear(type);
        return ResponseEntity.noContent().build();
    }

    private void requireOperator(Authentication authentication) {
        CurrentUser currentUser = CurrentUser.required(authentication);
        if (!currentUser.canManageCatalog()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Operator role required");
        }
    }

    public record FailureRequest(String type, boolean active) {}
}
