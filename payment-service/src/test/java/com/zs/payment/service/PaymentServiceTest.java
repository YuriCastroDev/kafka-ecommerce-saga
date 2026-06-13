package com.zs.payment.service;

import com.zs.payment.entity.Payment;
import com.zs.payment.event.PaymentResultEvent;
import com.zs.payment.event.StockReservedEvent;
import com.zs.payment.repository.PaymentRepository;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldSkipIfAlreadyProcessed() {
        UUID orderId = UUID.randomUUID();
        StockReservedEvent event = new StockReservedEvent(orderId, "Teclado", 1, BigDecimal.valueOf(150), "João");

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(Payment.builder().build()));

        paymentService.processPayment(event);

        verify(paymentRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void shouldPersistPaymentRecord() {
        UUID orderId = UUID.randomUUID();
        StockReservedEvent event = new StockReservedEvent(orderId, "Teclado", 1, BigDecimal.valueOf(150), "João");

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenReturn(Payment.builder().build());

        paymentService.processPayment(event);

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void shouldPublishPaymentResultEvent() {
        UUID orderId = UUID.randomUUID();
        StockReservedEvent event = new StockReservedEvent(orderId, "Teclado", 1, BigDecimal.valueOf(150), "João");

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenReturn(Payment.builder().build());

        paymentService.processPayment(event);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("payment-events"), eq(orderId.toString()), eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(PaymentResultEvent.class);
    }

    @RepeatedTest(20)
    void shouldPublishEitherConfirmedOrFailed() {
        UUID orderId = UUID.randomUUID();
        StockReservedEvent event = new StockReservedEvent(orderId, "Teclado", 1, BigDecimal.valueOf(150), "João");

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenReturn(Payment.builder().build());

        paymentService.processPayment(event);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("payment-events"), eq(orderId.toString()), eventCaptor.capture());

        PaymentResultEvent result = (PaymentResultEvent) eventCaptor.getValue();
        assertThat(result.status()).isIn("CONFIRMED", "FAILED");

        reset(paymentRepository, kafkaTemplate);
    }
}
