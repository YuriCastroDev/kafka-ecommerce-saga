package com.zs.stock.service;

import com.zs.stock.entity.StockItem;
import com.zs.stock.repository.StockItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final StockItemRepository stockItemRepository;

    @Override
    public void run(String... args) {
        if (stockItemRepository.count() == 0) {
            stockItemRepository.save(StockItem.builder().productName("Teclado").quantity(100).build());
            stockItemRepository.save(StockItem.builder().productName("Mouse").quantity(50).build());
            stockItemRepository.save(StockItem.builder().productName("Monitor").quantity(20).build());
            stockItemRepository.save(StockItem.builder().productName("Headset").quantity(30).build());
            log.info("Stock initialized with default products");
        }
    }
}
