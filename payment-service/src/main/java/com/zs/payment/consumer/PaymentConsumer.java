package com.zs.payment.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zs.payment.event.StockReservedEvent;
import com.zs.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @KafkaListener(topics = "stock-events", groupId = "payment-group")
    public void onStockReserved(String payload) {
        try {
            if (!payload.contains("customerName")) {
                log.debug("Ignoring non-StockReserved event");
                return;
            }
            StockReservedEvent event = objectMapper.readValue(payload, StockReservedEvent.class);
            log.info("Received StockReserved — orderId: {}", event.orderId());
            paymentService.processPayment(event);
        } catch (Exception e) {
            log.error("Error processing stock-events: {}", e.getMessage());
        }
    }
}