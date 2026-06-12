package com.zs.payment.service;

import com.zs.payment.entity.Payment;
import com.zs.payment.event.PaymentResultEvent;
import com.zs.payment.event.StockReservedEvent;
import com.zs.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();

    @Transactional
    public void processPayment(StockReservedEvent event) {
        // Idempotência
        if (paymentRepository.findByOrderId(event.orderId()).isPresent()) {
            log.warn("Payment already processed for orderId: {} — skipping", event.orderId());
            return;
        }

        // Simula 20% de falha no pagamento
        boolean failed = random.nextInt(100) < 20;

        Payment payment = Payment.builder()
                .orderId(event.orderId())
                .amount(event.price())
                .status(failed ? Payment.PaymentStatus.FAILED : Payment.PaymentStatus.CONFIRMED)
                .failureReason(failed ? "Insufficient funds (simulated)" : null)
                .processedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        if (failed) {
            log.warn("Payment FAILED for orderId: {}", event.orderId());
            kafkaTemplate.send("payment-events", event.orderId().toString(),
                    new PaymentResultEvent(
                            event.orderId(), "FAILED",
                            event.productName(), event.quantity(),
                            event.customerName(), event.price(),
                            "Insufficient funds (simulated)"
                    ));
        } else {
            log.info("Payment CONFIRMED for orderId: {}", event.orderId());
            kafkaTemplate.send("payment-events", event.orderId().toString(),
                    new PaymentResultEvent(
                            event.orderId(), "CONFIRMED",
                            event.productName(), event.quantity(),
                            event.customerName(), event.price(),
                            null
                    ));
        }
    }
}
