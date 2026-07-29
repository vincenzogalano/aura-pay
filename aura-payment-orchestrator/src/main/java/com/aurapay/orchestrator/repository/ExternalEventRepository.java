package com.aurapay.orchestrator.repository;

import com.aurapay.orchestrator.domain.ExternalEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExternalEventRepository extends JpaRepository<ExternalEvent, UUID> {
    boolean existsByEventTypeAndAggregateId(String eventType, String aggregateId);
}
