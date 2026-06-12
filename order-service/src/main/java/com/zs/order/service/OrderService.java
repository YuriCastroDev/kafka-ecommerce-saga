package com.zs.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zs.order.entity.Order;
import com.zs.order.entity.OrderRequest;
import com.zs.order.entity.OutboxEvent;
import com.zs.order.event.OrderCreatedEvent;
import com.zs.order.repository.OrderRepository;
import com.zs.order.repository.OutboxEventRepository;
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

        // Se o serviço cair aqui, o pedido e o evento rollback juntos
        // Se o serviço cair depois do commit, o OutboxPublisher vai retentar
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
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(saved.getId())
                    .eventType("OrderCreated")
                    .topic("order-events")
                    .payload(payload)
                    .build();

            outboxEventRepository.save(outboxEvent);
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
}
