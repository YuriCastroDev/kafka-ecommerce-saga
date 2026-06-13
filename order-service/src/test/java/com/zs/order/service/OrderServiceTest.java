package com.zs.order.service;

import com.zs.order.entity.Order;
import com.zs.order.entity.OrderRequest;
import com.zs.order.entity.OutboxEvent;
import com.zs.order.repository.OrderRepository;
import com.zs.order.repository.OutboxEventRepository;
import com.zs.order.entity.SagaState;
import com.zs.order.entity.SagaTracker;
import com.zs.order.repository.SagaTrackerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private SagaTrackerRepository sagaTrackerRepository;

    @InjectMocks
    private OrderService orderService;

    private Order savedOrder;
    private OrderRequest request;

    @BeforeEach
    void setUp() {
        request = new OrderRequest("João", "Teclado", 1, BigDecimal.valueOf(150));

        savedOrder = Order.builder()
                .id(UUID.randomUUID())
                .customerName("João")
                .productName("Teclado")
                .quantity(1)
                .price(BigDecimal.valueOf(150))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldCreateOrderAndSaveOutboxEventAndInitSaga() {
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        orderService.createOrder(request);

        verify(orderRepository).save(any(Order.class));
        verify(outboxEventRepository).save(any(OutboxEvent.class));

        ArgumentCaptor<SagaTracker> sagaCaptor = ArgumentCaptor.forClass(SagaTracker.class);
        verify(sagaTrackerRepository).save(sagaCaptor.capture());
        assertThat(sagaCaptor.getValue().getState()).isEqualTo(SagaState.PENDING);
    }

    @Test
    void shouldSaveOutboxEventWithCorrectTopic() {
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        orderService.createOrder(request);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());

        assertThat(outboxCaptor.getValue().getTopic()).isEqualTo("order-events");
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("OrderCreated");
        assertThat(outboxCaptor.getValue().getPayload()).contains("João");
    }

    @Test
    void shouldGetOrderById() {
        when(orderRepository.findById(savedOrder.getId())).thenReturn(Optional.of(savedOrder));

        Order result = orderService.getOrder(savedOrder.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(savedOrder.getId());
    }

    @Test
    void shouldThrowWhenOrderNotFound() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void shouldGetSagaStatus() {
        SagaTracker tracker = SagaTracker.builder()
                .orderId(savedOrder.getId())
                .state(SagaState.PAYMENT_CONFIRMED)
                .build();

        when(sagaTrackerRepository.findById(savedOrder.getId())).thenReturn(Optional.of(tracker));

        SagaTracker result = orderService.getSagaStatus(savedOrder.getId());

        assertThat(result.getState()).isEqualTo(SagaState.PAYMENT_CONFIRMED);
    }
}
