package com.orderflow.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.domain.Order;
import com.orderflow.domain.OrderItem;
import com.orderflow.domain.OrderStatus;
import com.orderflow.domain.InventoryReservationStatus;
import com.orderflow.outbox.OutboxEvent;
import com.orderflow.repository.InventoryRepository;
import com.orderflow.repository.InventoryReservationRepository;
import com.orderflow.repository.OrderRepository;
import com.orderflow.repository.OutboxRepository;
import com.orderflow.repository.ProcessedEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class OrderSagaConsumer {
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxRepository outboxRepository;

    public OrderSagaConsumer(ObjectMapper objectMapper, OrderRepository orderRepository, InventoryRepository inventoryRepository,
                             InventoryReservationRepository reservationRepository, ProcessedEventRepository processedEventRepository,
                             OutboxRepository outboxRepository) {
        this.objectMapper = objectMapper;
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.processedEventRepository = processedEventRepository;
        this.outboxRepository = outboxRepository;
    }

    @KafkaListener(topics = {"payment.authorized", "payment.failed"}, groupId = "${KAFKA_ORDER_GROUP:orderflow-order}")
    @Transactional
    public void handle(String rawEvent) throws Exception {
        EventEnvelope event = objectMapper.readValue(rawEvent, EventEnvelope.class);
        if (processedEventRepository.existsByEventIdAndConsumerName(event.eventId().toString(), "order-saga")) {
            return;
        }
        Order order = orderRepository.findByIdAndOrganizationId(event.aggregateId(), event.tenantId()).orElseThrow();
        if ("payment.authorized".equals(event.eventType())) {
            if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
                order.setUpdatedAt(Instant.now());
                orderRepository.save(order);
                emit(event, "order.confirmed", order, event.payload());
            }
        } else if (order.getStatus() == OrderStatus.INVENTORY_RESERVED || order.getStatus() == OrderStatus.PAYMENT_PENDING) {
            releaseInventory(order);
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);
            emit(event, "order.cancelled", order, event.payload());
        }
        markProcessed(event, "order-saga");
    }

    private void releaseInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            var reservation = reservationRepository.findByOrderIdAndProductIdAndOrganizationId(order.getId(), item.getProductId(), order.getOrganizationId());
            if (reservation.isEmpty() || reservation.get().getStatus() != InventoryReservationStatus.RESERVED) {
                continue;
            }
            inventoryRepository.lockByProductIdAndOrganizationId(item.getProductId(), order.getOrganizationId()).ifPresent(inventory -> {
                inventory.setAvailableQuantity(inventory.getAvailableQuantity() + item.getQuantity());
                inventory.setReservedQuantity(Math.max(0, inventory.getReservedQuantity() - item.getQuantity()));
                inventory.setUpdatedAt(Instant.now());
                inventoryRepository.save(inventory);
                reservation.get().setStatus(InventoryReservationStatus.RELEASED);
                reservation.get().setUpdatedAt(Instant.now());
                reservationRepository.save(reservation.get());
            });
        }
    }

    private void emit(EventEnvelope source, String type, Order order, Object payload) throws Exception {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(order.getId());
        event.setAggregateType("ORDER");
        event.setEventType(type);
        event.setCreatedAt(Instant.now());
        event.setRetryCount(0);
        event.setPayload(objectMapper.writeValueAsString(new EventEnvelope(
            event.getId(), type, order.getId(), "ORDER", event.getCreatedAt(), 1,
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
