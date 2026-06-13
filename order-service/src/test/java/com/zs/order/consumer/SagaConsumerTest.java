package com.zs.order.consumer;

import com.zs.order.entity.SagaState;
import com.zs.order.entity.SagaTracker;
import com.zs.order.repository.SagaTrackerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaConsumerTest {

    @Mock
    private SagaTrackerRepository sagaTrackerRepository;

    @InjectMocks
    private SagaConsumer sagaConsumer;

    @Test
    void shouldUpdateStateToStockReservedOnStockReservedEvent() {
        UUID orderId = UUID.randomUUID();
        SagaTracker tracker = SagaTracker.builder().orderId(orderId).state(SagaState.PENDING).build();
        when(sagaTrackerRepository.findById(orderId)).thenReturn(Optional.of(tracker));

        String payload = """
                {"orderId":"%s","productName":"Teclado","quantity":1,"price":150,"customerName":"João"}
                """.formatted(orderId);

        sagaConsumer.onStockEvent(payload);

        ArgumentCaptor<SagaTracker> captor = ArgumentCaptor.forClass(SagaTracker.class);
        verify(sagaTrackerRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(SagaState.STOCK_RESERVED);
    }

    @Test
    void shouldUpdateStateToCancelledOnStockFailedEvent() {
        UUID orderId = UUID.randomUUID();
        SagaTracker tracker = SagaTracker.builder().orderId(orderId).state(SagaState.PENDING).build();
        when(sagaTrackerRepository.findById(orderId)).thenReturn(Optional.of(tracker));

        String payload = """
                {"orderId":"%s","reason":"Insufficient stock"}
                """.formatted(orderId);

        sagaConsumer.onStockEvent(payload);

        ArgumentCaptor<SagaTracker> captor = ArgumentCaptor.forClass(SagaTracker.class);
        verify(sagaTrackerRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(SagaState.CANCELLED);
    }

    @Test
    void shouldUpdateStateToPaymentConfirmedOnConfirmedEvent() {
        UUID orderId = UUID.randomUUID();
        SagaTracker tracker = SagaTracker.builder().orderId(orderId).state(SagaState.STOCK_RESERVED).build();
        when(sagaTrackerRepository.findById(orderId)).thenReturn(Optional.of(tracker));

        String payload = """
                {"orderId":"%s","status":"CONFIRMED","customerName":"João"}
                """.formatted(orderId);

        sagaConsumer.onPaymentEvent(payload);

        ArgumentCaptor<SagaTracker> captor = ArgumentCaptor.forClass(SagaTracker.class);
        verify(sagaTrackerRepository).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(SagaState.PAYMENT_CONFIRMED);
    }

    @Test
    void shouldUpdateStateToCancelledOnPaymentFailedEvent() {
        UUID orderId = UUID.randomUUID();
        SagaTracker tracker = SagaTracker.builder().orderId(orderId).state(SagaState.STOCK_RESERVED).build();
        when(sagaTrackerRepository.findById(orderId)).thenReturn(Optional.of(tracker));

        String payload = """
                {"orderId":"%s","status":"FAILED","reason":"Insufficient funds"}
                """.formatted(orderId);

        sagaConsumer.onPaymentEvent(payload);

        ArgumentCaptor<SagaTracker> captor = ArgumentCaptor.forClass(SagaTracker.class);
        verify(sagaTrackerRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(SagaState.CANCELLED);
    }
}
