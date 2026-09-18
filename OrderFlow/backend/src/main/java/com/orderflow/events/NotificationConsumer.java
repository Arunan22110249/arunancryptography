package com.orderflow.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.domain.Notification;
import com.orderflow.domain.ProcessedEvent;
import com.orderflow.repository.NotificationRepository;
import com.orderflow.repository.ProcessedEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class NotificationConsumer {
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;

    public NotificationConsumer(ObjectMapper objectMapper, NotificationRepository notificationRepository,
                                ProcessedEventRepository processedEventRepository) {
        this.objectMapper = objectMapper;
        this.notificationRepository = notificationRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @KafkaListener(topics = {"order.confirmed", "order.cancelled"}, groupId = "${KAFKA_NOTIFICATION_GROUP:orderflow-notification}")
    @Transactional
    public void handle(String rawEvent) throws Exception {
        EventEnvelope event = objectMapper.readValue(rawEvent, EventEnvelope.class);
        if (processedEventRepository.existsByEventIdAndConsumerName(event.eventId().toString(), "notification")) return;
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setOrganizationId(event.tenantId());
        notification.setOrderId(event.aggregateId());
        notification.setChannel("IN_APP");
        notification.setStatus("CREATED");
        notification.setPayload(objectMapper.writeValueAsString(event.payload()));
        notification.setCreatedAt(Instant.now());
        notificationRepository.save(notification);
        markProcessed(event);
    }

    private void markProcessed(EventEnvelope event) {
        ProcessedEvent processed = new ProcessedEvent();
        processed.setId(UUID.randomUUID());
        processed.setEventId(event.eventId().toString());
        processed.setConsumerName("notification");
        processed.setProcessedAt(Instant.now());
        processedEventRepository.save(processed);
    }
}
