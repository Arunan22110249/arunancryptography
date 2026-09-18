package com.orderflow.failure;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FailureModeService {
    private final boolean enabled;
    private final Map<String, Boolean> modes = new ConcurrentHashMap<>();

    public FailureModeService(@Value("${app.failure-injection.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() { return enabled; }
    public Map<String, Boolean> all() { return Map.copyOf(modes); }
    public boolean isActive(String type) { return enabled && modes.getOrDefault(type, false); }
    public void set(String type, boolean active) {
        if (!enabled) {
            throw new IllegalStateException("Failure injection is disabled");
        }
        modes.put(type, active);
    }
    public void clear(String type) { modes.remove(type); }
}
