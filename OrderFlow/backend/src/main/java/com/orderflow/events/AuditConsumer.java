package com.orderflow.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.domain.AuditEvent;
import com.orderflow.domain.ProcessedEvent;
import com.orderflow.repository.AuditEventRepository;
import com.orderflow.repository.ProcessedEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class AuditConsumer {
    private final ObjectMapper objectMapper;
    private final AuditEventRepository auditEventRepository;
    private final ProcessedEventRepository processedEventRepository;

    public AuditConsumer(ObjectMapper objectMapper, AuditEventRepository auditEventRepository,
                         ProcessedEventRepository processedEventRepository) {
        this.objectMapper = objectMapper;
        this.auditEventRepository = auditEventRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = {"order.created", "payment.authorized", "payment.failed", "order.confirmed", "order.cancelled"}, groupId = "${KAFKA_AUDIT_GROUP:orderflow-audit}")
    @Transactional
    public void handle(String rawEvent) throws Exception {
        EventEnvelope event = objectMapper.readValue(rawEvent, EventEnvelope.class);
        if (processedEventRepository.existsByEventIdAndConsumerName(event.eventId().toString(), "audit")) return;
        AuditEvent audit = new AuditEvent();
        audit.setId(UUID.randomUUID());
        audit.setActor("event-consumer");
        audit.setTenant(event.tenantId().toString());
        audit.setAction(event.eventType());
        audit.setResource(event.aggregateType() + ":" + event.aggregateId());
        audit.setTimestamp(event.timestamp());
        audit.setCorrelationId(event.correlationId());
        audit.setMetadata(objectMapper.writeValueAsString(event.payload()));
        auditEventRepository.save(audit);
        ProcessedEvent processed = new ProcessedEvent();
        processed.setId(UUID.randomUUID());
        processed.setEventId(event.eventId().toString());
        processed.setConsumerName("audit");
        processed.setProcessedAt(Instant.now());
        processedEventRepository.save(processed);
    }
}
