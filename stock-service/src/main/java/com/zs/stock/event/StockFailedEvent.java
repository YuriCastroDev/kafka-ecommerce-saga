package com.zs.stock.event;

import java.util.UUID;

public record StockFailedEvent(
        UUID orderId,
        String reason
) {}
