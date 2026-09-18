package com.orderflow.events;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(
    UUID eventId,
    String eventType,
    UUID aggregateId,
    String aggregateType,
    Instant timestamp,
    int version,
    String correlationId,
    String causationId,
    UUID tenantId,
    Object payload
) {}
