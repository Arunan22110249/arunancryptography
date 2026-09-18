package com.orderflow.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.api.dto.CreateOrderRequest;
import com.orderflow.api.dto.OrderResponse;
import com.orderflow.domain.IdempotencyKeyRecord;
import com.orderflow.domain.InventoryReservation;
import com.orderflow.domain.InventoryReservationStatus;
import com.orderflow.domain.Order;
import com.orderflow.domain.OrderItem;
import com.orderflow.domain.OrderStatus;
import com.orderflow.events.EventEnvelope;
import com.orderflow.outbox.OutboxEvent;
import com.orderflow.repository.IdempotencyRepository;
import com.orderflow.repository.InventoryRepository;
import com.orderflow.repository.InventoryReservationRepository;
import com.orderflow.repository.OrderRepository;
import com.orderflow.repository.OutboxRepository;
import com.orderflow.repository.ProductRepository;
import com.orderflow.security.CurrentUser;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderController(OrderRepository orderRepository, ProductRepository productRepository, InventoryRepository inventoryRepository,
                           InventoryReservationRepository reservationRepository, IdempotencyRepository idempotencyRepository,
                           OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> listOrders(Authentication authentication) {
        CurrentUser currentUser = CurrentUser.required(authentication);
        return ResponseEntity.ok(orderRepository.findAllByOrganizationIdOrderByCreatedAtDesc(currentUser.tenantId()).stream().map(OrderResponse::fromEntity).toList());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id, Authentication authentication) {
        CurrentUser currentUser = CurrentUser.required(authentication);
        return orderRepository.findByIdAndOrganizationId(id, currentUser.tenantId())
            .map(OrderResponse::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Transactional
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @RequestBody CreateOrderRequest request,
        Authentication authentication
    ) {
        CurrentUser currentUser = CurrentUser.required(authentication);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        String requestHash = request.items().toString();
        var existing = idempotencyRepository.findByKeyValue(idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            try {
                return ResponseEntity.ok(objectMapper.readValue(existing.get().getResponse(), OrderResponse.class));
            } catch (JsonProcessingException ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setOrganizationId(currentUser.tenantId());
        order.setCustomerId(currentUser.userId());
        order.setStatus(OrderStatus.PENDING);
        order.setCurrency("USD");
        order.setTotalAmount(BigDecimal.ZERO);
        Instant now = Instant.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        for (CreateOrderRequest.Item item : request.items()) {
            var product = productRepository.findByIdAndOrganizationId(item.productId(), currentUser.tenantId()).orElseThrow();
            var inventory = inventoryRepository.findByProductIdAndOrganizationId(item.productId(), order.getOrganizationId())
                .orElseThrow();
            if (inventory.getAvailableQuantity() < item.quantity()) {
                throw new IllegalStateException("Insufficient inventory");
            }
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - item.quantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.quantity());
            inventory.setUpdatedAt(now);
            inventoryRepository.save(inventory);
            InventoryReservation reservation = new InventoryReservation();
            reservation.setId(UUID.randomUUID());
            reservation.setOrderId(order.getId());
            reservation.setOrganizationId(order.getOrganizationId());
            reservation.setProductId(item.productId());
            reservation.setQuantity(item.quantity());
            reservation.setStatus(InventoryReservationStatus.RESERVED);
            reservation.setCreatedAt(now);
            reservation.setUpdatedAt(now);
            reservationRepository.save(reservation);
            order.setTotalAmount(order.getTotalAmount().add(product.getPrice().multiply(BigDecimal.valueOf(item.quantity()))));

            OrderItem orderItem = new OrderItem();
            orderItem.setId(UUID.randomUUID());
            orderItem.setOrder(order);
            orderItem.setProductId(item.productId());
            orderItem.setQuantity(item.quantity());
            orderItem.setUnitPrice(product.getPrice());
            order.getItems().add(orderItem);
        }
        order.setStatus(OrderStatus.INVENTORY_RESERVED);
        orderRepository.save(order);
        OrderResponse response = OrderResponse.fromEntity(order);
        try {
            IdempotencyKeyRecord record = new IdempotencyKeyRecord();
            record.setId(UUID.randomUUID());
            record.setKeyValue(idempotencyKey);
            record.setRequestHash(requestHash);
            record.setResponse(objectMapper.writeValueAsString(response));
            record.setOrderId(order.getId());
            record.setCreatedAt(now);
            record.setExpiresAt(now.plusSeconds(86400));
            idempotencyRepository.save(record);

            OutboxEvent event = new OutboxEvent();
            event.setId(UUID.randomUUID());
            event.setAggregateId(order.getId());
            event.setAggregateType("ORDER");
            event.setEventType("order.created");
            event.setPayload(objectMapper.writeValueAsString(new EventEnvelope(
                event.getId(), "order.created", order.getId(), "ORDER", now, 1,
                order.getId().toString(), null, order.getOrganizationId(), response)));
            event.setCreatedAt(now);
            event.setRetryCount(0);
            outboxRepository.save(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to persist order event", ex);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
