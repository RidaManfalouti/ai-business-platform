package com.aibi.sales_service.saga.repository;

import com.aibi.sales_service.saga.entity.SagaEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SagaEventRepository extends JpaRepository<SagaEvent, Long> {

    List<SagaEvent> findByTenantIdAndSagaIdOrderByCreatedAtAsc(UUID tenantId, String sagaId);

    List<SagaEvent> findBySagaIdOrderByCreatedAtAsc(String sagaId);
}
