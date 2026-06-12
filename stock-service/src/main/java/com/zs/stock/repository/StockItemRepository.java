package com.zs.stock.repository;

import com.zs.stock.entity.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockItemRepository extends JpaRepository<StockItem, UUID> {
    Optional<StockItem> findByProductName(String productName);
}
