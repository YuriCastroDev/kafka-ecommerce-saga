package com.zs.stock.service;

import com.zs.stock.entity.StockItem;
import com.zs.stock.entity.StockReservation;
import com.zs.stock.event.OrderCreatedEvent;
import com.zs.stock.event.StockFailedEvent;
import com.zs.stock.event.StockReservedEvent;
import com.zs.stock.repository.StockItemRepository;
import com.zs.stock.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockItemRepository stockItemRepository;
    private final StockReservationRepository reservationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void reserveStock(OrderCreatedEvent event) {
        // Idempotência — ignora se já processou esse pedido
        if (reservationRepository.findByOrderId(event.orderId()).isPresent()) {
            log.warn("Stock already processed for orderId: {} — skipping", event.orderId());
            return;
        }

        Optional<StockItem> stockOpt = stockItemRepository.findByProductName(event.productName());

        if (stockOpt.isEmpty() || stockOpt.get().getQuantity() < event.quantity()) {
            log.warn("Stock unavailable for product: {} | orderId: {}", event.productName(), event.orderId());
            kafkaTemplate.send("stock-events", event.orderId().toString(),
                    new StockFailedEvent(event.orderId(), "Insufficient stock for: " + event.productName()));
            return;
        }

        StockItem stock = stockOpt.get();
        stock.setQuantity(stock.getQuantity() - event.quantity());
        stockItemRepository.save(stock);

        reservationRepository.save(StockReservation.builder()
                .orderId(event.orderId())
                .productName(event.productName())
                .quantity(event.quantity())
                .status(StockReservation.ReservationStatus.RESERVED)
                .build());

        log.info("Stock reserved for orderId: {} | product: {}", event.orderId(), event.productName());
        kafkaTemplate.send("stock-events", event.orderId().toString(),
                new StockReservedEvent(event.orderId(), event.productName(), event.quantity(), event.price(), event.customerName()));
    }

    @Transactional
    public void releaseStock(String productName, Integer quantity, java.util.UUID orderId) {
        stockItemRepository.findByProductName(productName).ifPresent(stock -> {
            stock.setQuantity(stock.getQuantity() + quantity);
            stockItemRepository.save(stock);
            log.info("Stock released for orderId: {} | product: {}", orderId, productName);
        });

        reservationRepository.findByOrderId(orderId).ifPresent(reservation -> {
            reservation.setStatus(StockReservation.ReservationStatus.RELEASED);
            reservationRepository.save(reservation);
        });
    }
}
