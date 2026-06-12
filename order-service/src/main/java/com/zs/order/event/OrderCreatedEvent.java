package com.zs.order.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String customerName,
        String productName,
        Integer quantity,
        BigDecimal price,
        LocalDateTime createdAt
) {}
