package com.zs.stock.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "stock_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String productName;

    private Integer quantity;
}
