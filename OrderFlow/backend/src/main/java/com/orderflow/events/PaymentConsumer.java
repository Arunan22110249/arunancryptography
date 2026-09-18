package com.orderflow.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.domain.Order;
import com.orderflow.domain.OrderStatus;
import com.orderflow.domain.Payment;
import com.orderflow.domain.PaymentStatus;
import com.orderflow.failure.FailureModeService;
import com.orderflow.outbox.OutboxEvent;
import com.orderflow.repository.OrderRepository;
import com.orderflow.repository.OutboxRepository;
import com.orderflow.repository.PaymentRepository;
import com.orderflow.repository.ProcessedEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class PaymentConsumer {
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxRepository outboxRepository;
    private final FailureModeService failureModeService;

    public PaymentConsumer(ObjectMapper objectMapper, OrderRepository orderRepository, PaymentRepository paymentRepository,
                           ProcessedEventRepository processedEventRepository, OutboxRepository outboxRepository,
                           FailureModeService failureModeService) {
        this.objectMapper = objectMapper;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.processedEventRepository = processedEventRepository;
        this.outboxRepository = outboxRepository;
        this.failureModeService = failureModeService;
    }

    @KafkaListener(topics = "order.created", groupId = "${KAFKA_PAYMENT_GROUP:orderflow-payment}")
    @Transactional
    public void handle(String rawEvent) throws Exception {
        EventEnvelope event = objectMapper.readValue(rawEvent, EventEnvelope.class);
        if (processedEventRepository.existsByEventIdAndConsumerName(event.eventId().toString(), "payment")) return;
        JsonNode payload = objectMapper.valueToTree(event.payload());
        Order order = orderRepository.findByIdAndOrganizationId(event.aggregateId(), event.tenantId()).orElseThrow();
        Payment payment = paymentRepository.findByOrderIdAndOrganizationId(order.getId(), event.tenantId()).orElseGet(() -> newPayment(order));
        if (payment.getStatus() != PaymentStatus.PENDING) {
            markProcessed(event, "payment");
            return;
        }
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        Instant now = Instant.now();
        payment.setUpdatedAt(now);
        String mode = failureModeService.isActive("payment_timeout") ? "timeout"
            : failureModeService.isActive("payment_failure") ? "failed" : "none";
        PaymentStatus result = switch (mode) {
            case "timeout" -> PaymentStatus.TIMEOUT;
            case "failed" -> PaymentStatus.FAILED;
            default -> PaymentStatus.AUTHORIZED;
        };
        payment.setStatus(result);
        paymentRepository.save(payment);
        orderRepository.save(order);
        emit(event, result == PaymentStatus.AUTHORIZED ? "payment.authorized" : "payment.failed", order, payload, now);
        markProcessed(event, "payment");
    }

    private Payment newPayment(Order order) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(order.getId());
        payment.setOrganizationId(order.getOrganizationId());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency(order.getCurrency());
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(payment.getCreatedAt());
        return payment;
    }

    private void emit(EventEnvelope source, String type, Order order, JsonNode payload, Instant timestamp) throws Exception {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(order.getId());
        event.setAggregateType("ORDER");
        event.setEventType(type);
        event.setCreatedAt(timestamp);
        event.setRetryCount(0);
        event.setPayload(objectMapper.writeValueAsString(new EventEnvelope(
            event.getId(), type, order.getId(), "ORDER", timestamp, 1,
            source.correlationId(), source.eventId().toString(), order.getOrganizationId(), payload)));
        outboxRepository.save(event);
    }

    private void markProcessed(EventEnvelope event, String consumer) {
        com.orderflow.domain.ProcessedEvent processed = new com.orderflow.domain.ProcessedEvent();
        processed.setId(UUID.randomUUID());
        processed.setEventId(event.eventId().toString());
        processed.setConsumerName(consumer);
        processed.setProcessedAt(Instant.now());
        processedEventRepository.save(processed);
    }
}
