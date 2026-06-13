package com.zs.order.consumer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zs.order.entity.SagaState;
import com.zs.order.entity.SagaTracker;
import com.zs.order.repository.SagaTrackerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaConsumer {

    private final SagaTrackerRepository sagaTrackerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @KafkaListener(topics = "stock-events", groupId = "order-saga-stock-group")
    public void onStockEvent(String payload) {
        try {
            Map<?, ?> map = objectMapper.readValue(payload, Map.class);
            UUID orderId = UUID.fromString((String) map.get("orderId"));

            if (map.containsKey("reason")) {
                updateState(orderId, SagaState.STOCK_FAILED);
                updateState(orderId, SagaState.CANCELLED);
            } else {
                updateState(orderId, SagaState.STOCK_RESERVED);
            }
        } catch (Exception e) {
            log.error("Error processing stock-events in saga: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "order-saga-payment-group")
    public void onPaymentEvent(String payload) {
        try {
            Map<?, ?> map = objectMapper.readValue(payload, Map.class);
            UUID orderId = UUID.fromString((String) map.get("orderId"));
            String status = (String) map.get("status");

            if ("CONFIRMED".equals(status)) {
                updateState(orderId, SagaState.PAYMENT_CONFIRMED);
            } else {
                updateState(orderId, SagaState.PAYMENT_FAILED);
                updateState(orderId, SagaState.CANCELLED);
            }
        } catch (Exception e) {
            log.error("Error processing payment-events in saga: {}", e.getMessage());
        }
    }

    private void updateState(UUID orderId, SagaState state) {
        SagaTracker tracker = sagaTrackerRepository.findById(orderId)
                .orElse(SagaTracker.builder().orderId(orderId).build());
        tracker.setState(state);
        sagaTrackerRepository.save(tracker);
        log.info("Saga state updated — orderId: {} | state: {}", orderId, state);
    }
}
