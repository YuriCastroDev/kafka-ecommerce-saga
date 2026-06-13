package com.zs.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zs.notification.entity.NotificationLog;
import com.zs.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationLogRepository notificationLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @KafkaListener(topics = "payment-events", groupId = "notification-payment-group")
    public void onPaymentEvent(String payload) {
        try {
            Map<?, ?> map = objectMapper.readValue(payload, Map.class);
            String status = (String) map.get("status");
            UUID orderId = UUID.fromString((String) map.get("orderId"));
            String customerName = (String) map.get("customerName");

            if ("CONFIRMED".equals(status)) {
                String message = "Your order has been confirmed! Payment processed successfully.";
                log.info("📧 Notifying {} — ORDER CONFIRMED for orderId: {}", customerName, orderId);
                saveLog(orderId, customerName, message, "ORDER_CONFIRMED");
            } else if ("FAILED".equals(status)) {
                String reason = (String) map.get("reason");
                String message = "Unfortunately your order was cancelled. Reason: " + reason;
                log.warn("📧 Notifying {} — ORDER CANCELLED for orderId: {}", customerName, orderId);
                saveLog(orderId, customerName, message, "ORDER_CANCELLED");
            }
        } catch (Exception e) {
            log.error("Error processing payment-events notification: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "stock-events", groupId = "notification-stock-group")
    public void onStockEvent(String payload) {
        try {
            Map<?, ?> map = objectMapper.readValue(payload, Map.class);

            // Só notifica StockFailed (StockReserved não precisa notificar)
            if (!map.containsKey("reason")) return;

            UUID orderId = UUID.fromString((String) map.get("orderId"));
            String reason = (String) map.get("reason");
            String message = "Your order could not be processed. " + reason;

            log.warn("📧 Notifying customer — STOCK FAILED for orderId: {}", orderId);
            saveLog(orderId, "Customer", message, "STOCK_FAILED");
        } catch (Exception e) {
            log.error("Error processing stock-events notification: {}", e.getMessage());
        }
    }

    private void saveLog(UUID orderId, String customerName, String message, String type) {
        notificationLogRepository.save(NotificationLog.builder()
                .orderId(orderId)
                .customerName(customerName)
                .message(message)
                .type(type)
                .build());
    }
}
