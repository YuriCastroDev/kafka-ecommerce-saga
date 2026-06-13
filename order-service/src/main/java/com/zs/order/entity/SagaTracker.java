package com.zs.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saga_trackers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaTracker {

    @Id
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private SagaState state;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
