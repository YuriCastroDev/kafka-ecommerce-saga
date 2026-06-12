package com.zs.stock.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zs.stock.event.OrderCreatedEvent;
import com.zs.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockConsumer {

    private final StockService stockService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @KafkaListener(topics = "order-events", groupId = "stock-group")
    public void onOrderCreated(String payload) {
        try {
            log.info("RAW PAYLOAD: {}", payload);
            log.info("PAYLOAD CLASS: {}", payload.getClass().getName());

            // Remove aspas externas se o payload vier escapado
            String json = payload.trim();
            if (json.startsWith("\"")) {
                json = objectMapper.readValue(json, String.class);
            }

            OrderCreatedEvent event = objectMapper.readValue(json, OrderCreatedEvent.class);
            log.info("Received OrderCreated — orderId: {}", event.orderId());
            stockService.reserveStock(event);
        } catch (Exception e) {
            log.error("Error processing order-events: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "stock-compensation-group")
    public void onPaymentFailed(String payload) {
        try {
            Map<?, ?> map = objectMapper.readValue(payload, Map.class);
            String status = (String) map.get("status");

            if ("FAILED".equals(status)) {
                UUID orderId = UUID.fromString((String) map.get("orderId"));
                String productName = (String) map.get("productName");
                Integer quantity = (Integer) map.get("quantity");

                log.warn("Payment failed for orderId: {} — releasing stock", orderId);
                stockService.releaseStock(productName, quantity, orderId);
            }
        } catch (Exception e) {
            log.error("Error processing payment-events for compensation: {}", e.getMessage());
        }
    }
}