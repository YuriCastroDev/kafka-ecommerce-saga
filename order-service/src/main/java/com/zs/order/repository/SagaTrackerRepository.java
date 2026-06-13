package com.zs.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zs.order.entity.SagaTracker;

import java.util.UUID;

public interface SagaTrackerRepository extends JpaRepository<SagaTracker, UUID> {
}
