package com.zs.stock.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize
public record OrderCreatedEvent(
        UUID orderId,
        String customerName,
        String productName,
        Integer quantity,
        BigDecimal price,
        LocalDateTime createdAt
) {}