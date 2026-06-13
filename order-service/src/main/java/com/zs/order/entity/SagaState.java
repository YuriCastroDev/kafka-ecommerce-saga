package com.zs.order.entity;

public enum SagaState {
    PENDING,
    STOCK_RESERVED,
    STOCK_FAILED,
    PAYMENT_CONFIRMED,
    PAYMENT_FAILED,
    CANCELLED
}
