package com.orderflow.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.domain.ProcessedEvent;
import com.orderflow.repository.ProcessedEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class AnalyticsConsumer {
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;
    private final Counter ordersCreated;
    private final Counter ordersConfirmed;
    private final Counter ordersCancelled;

    public AnalyticsConsumer(ObjectMapper objectMapper, ProcessedEventRepository processedEventRepository, MeterRegistry registry) {
        this.objectMapper = objectMapper;
        this.processedEventRepository = processedEventRepository;
        this.ordersCreated = registry.counter("orders_created_total");
        this.ordersConfirmed = registry.counter("orders_confirmed_total");
        this.ordersCancelled = registry.counter("orders_cancelled_total");
    }

    @KafkaListener(topics = {"order.created", "order.confirmed", "order.cancelled"}, groupId = "${KAFKA_ANALYTICS_GROUP:orderflow-analytics}")
    @Transactional
    public void handle(String rawEvent) throws Exception {
        EventEnvelope event = objectMapper.readValue(rawEvent, EventEnvelope.class);
        if (processedEventRepository.existsByEventIdAndConsumerName(event.eventId().toString(), "analytics")) return;
        switch (event.eventType()) {
            case "order.created" -> ordersCreated.increment();
            case "order.confirmed" -> ordersConfirmed.increment();
            case "order.cancelled" -> ordersCancelled.increment();
            default -> { return; }
        }
        ProcessedEvent processed = new ProcessedEvent();
        processed.setId(UUID.randomUUID());
        processed.setEventId(event.eventId().toString());
        processed.setConsumerName("analytics");
        processed.setProcessedAt(Instant.now());
        processedEventRepository.save(processed);
    }
}
