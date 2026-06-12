package com.zs.stock.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "stock_reservations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID orderId;
    private String productName;
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    public enum ReservationStatus {
        RESERVED, RELEASED
    }
}
