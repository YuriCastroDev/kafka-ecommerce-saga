package com.zs.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zs.order.entity.Order;
import com.zs.order.entity.OrderRequest;
import com.zs.order.entity.OutboxEvent;
import com.zs.order.event.OrderCreatedEvent;
import com.zs.order.repository.OrderRepository;
import com.zs.order.repository.OutboxEventRepository;
import com.zs.order.entity.SagaState;
import com.zs.order.entity.SagaTracker;
import com.zs.order.repository.SagaTrackerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final SagaTrackerRepository sagaTrackerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Transactional
    public Order createOrder(OrderRequest request) {
        Order order = Order.builder()
                .customerName(request.customerName())
                .productName(request.productName())
                .quantity(request.quantity())
                .price(request.price())
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order created — id: {}", saved.getId());

        sagaTrackerRepository.save(SagaTracker.builder()
                .orderId(saved.getId())
                .state(SagaState.PENDING)
                .build());

        OrderCreatedEvent event = new OrderCreatedEvent(
                saved.getId(),
                saved.getCustomerName(),
                saved.getProductName(),
                saved.getQuantity(),
                saved.getPrice(),
                saved.getCreatedAt()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxEventRepository.save(OutboxEvent.builder()
                    .aggregateId(saved.getId())
                    .eventType("OrderCreated")
                    .topic("order-events")
                    .payload(payload)
                    .build());
            log.info("Outbox event saved for orderId: {}", saved.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize OrderCreatedEvent", e);
        }

        return saved;
    }

    public Order getOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    public SagaTracker getSagaStatus(UUID orderId) {
        return sagaTrackerRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Saga not found for orderId: " + orderId));
    }
}
