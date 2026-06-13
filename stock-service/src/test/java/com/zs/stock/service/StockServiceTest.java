package com.zs.stock.service;

import com.zs.stock.entity.StockItem;
import com.zs.stock.entity.StockReservation;
import com.zs.stock.event.OrderCreatedEvent;
import com.zs.stock.event.StockFailedEvent;
import com.zs.stock.event.StockReservedEvent;
import com.zs.stock.repository.StockItemRepository;
import com.zs.stock.repository.StockReservationRepository;
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
class StockServiceTest {

    @Mock
    private StockItemRepository stockItemRepository;

    @Mock
    private StockReservationRepository reservationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private StockService stockService;

    @Test
    void shouldReserveStockAndPublishStockReservedEvent() {
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, "João", "Teclado", 1, BigDecimal.valueOf(150), null);

        StockItem stock = StockItem.builder().productName("Teclado").quantity(10).build();
        when(stockItemRepository.findByProductName("Teclado")).thenReturn(Optional.of(stock));
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        stockService.reserveStock(event);

        verify(stockItemRepository).save(any(StockItem.class));
        verify(reservationRepository).save(any(StockReservation.class));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("stock-events"), eq(orderId.toString()), eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(StockReservedEvent.class);
    }

    @Test
    void shouldPublishStockFailedWhenNoStock() {
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, "João", "Teclado", 5, BigDecimal.valueOf(150), null);

        StockItem stock = StockItem.builder().productName("Teclado").quantity(2).build();
        when(stockItemRepository.findByProductName("Teclado")).thenReturn(Optional.of(stock));
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        stockService.reserveStock(event);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("stock-events"), eq(orderId.toString()), eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(StockFailedEvent.class);
        verify(stockItemRepository, never()).save(any());
    }

    @Test
    void shouldSkipIfAlreadyProcessed() {
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, "João", "Teclado", 1, BigDecimal.valueOf(150), null);

        when(reservationRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(StockReservation.builder().build()));

        stockService.reserveStock(event);

        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(stockItemRepository, never()).save(any());
    }

    @Test
    void shouldDecreaseStockQuantityOnReservation() {
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, "João", "Teclado", 3, BigDecimal.valueOf(150), null);

        StockItem stock = StockItem.builder().productName("Teclado").quantity(10).build();
        when(stockItemRepository.findByProductName("Teclado")).thenReturn(Optional.of(stock));
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        stockService.reserveStock(event);

        ArgumentCaptor<StockItem> stockCaptor = ArgumentCaptor.forClass(StockItem.class);
        verify(stockItemRepository).save(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getQuantity()).isEqualTo(7);
    }
}
