package com.zs.stock.event;

import java.math.BigDecimal;
import java.util.UUID;

public record StockReservedEvent(
        UUID orderId,
        String productName,
        Integer quantity,
        BigDecimal price,
        String customerName
) {}
