package com.zs.payment.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResultEvent(
        UUID orderId,
        String status,
        String productName,
        Integer quantity,
        String customerName,
        BigDecimal amount,
        String reason
) {}
