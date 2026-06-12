package com.zs.order.entity;

import java.math.BigDecimal;

public record OrderRequest(
        String customerName,
        String productName,
        Integer quantity,
        BigDecimal price
) {}
