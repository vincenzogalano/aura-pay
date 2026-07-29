package com.aurapay.orchestrator.repository;

import com.aurapay.orchestrator.domain.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {
    List<PaymentIntent> findByIsTestOrderByCreatedAtDesc(boolean isTest);
}
