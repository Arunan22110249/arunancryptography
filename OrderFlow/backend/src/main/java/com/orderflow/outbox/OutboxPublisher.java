package com.orderflow.outbox;

import com.orderflow.repository.OutboxRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Counter publishSuccess;
    private final Counter publishFailure;
    private final Counter publishRetry;

    public OutboxPublisher(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
                           MeterRegistry meterRegistry) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.publishSuccess = meterRegistry.counter("outbox_publish_success_total");
        this.publishFailure = meterRegistry.counter("outbox_publish_failure_total");
        this.publishRetry = meterRegistry.counter("outbox_publish_retry_total");
        Gauge.builder("outbox_pending_events", outboxRepository, repository -> repository.findByPublishedAtIsNullOrderByCreatedAtAsc().size())
            .description("Number of outbox events waiting for Kafka acknowledgement")
            .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:1000}")
    public void publishPending() {
        List<OutboxEvent> events = outboxRepository.findByPublishedAtIsNullOrderByCreatedAtAsc();
        events.stream().limit(100).forEach(this::publish);
    }

    private void publish(OutboxEvent event) {
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
            event.getEventType(), event.getAggregateId().toString(), event.getPayload());
        future.whenComplete((result, error) -> {
            if (error == null) {
                markPublished(event, result);
            } else {
                markFailed(event, error);
            }
        });
    }

    @Transactional
    protected void markPublished(OutboxEvent event, SendResult<String, String> result) {
        event.setPublishedAt(Instant.now());
        event.setLastError(null);
        outboxRepository.save(event);
        publishSuccess.increment();
        RecordMetadata metadata = result.getRecordMetadata();
        log.info("outbox_event_published eventId={} topic={} partition={} offset={}", event.getId(),
            metadata.topic(), metadata.partition(), metadata.offset());
    }

    @Transactional
    protected void markFailed(OutboxEvent event, Throwable error) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastError(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
        outboxRepository.save(event);
        publishFailure.increment();
        publishRetry.increment();
        log.warn("outbox_event_publish_failed eventId={} retryCount={} error={}", event.getId(), event.getRetryCount(), event.getLastError());
    }
}
