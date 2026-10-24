package com.aibi.sales_service.saga.repository;

import com.aibi.sales_service.saga.entity.SagaEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SagaEventRepository extends JpaRepository<SagaEvent, Long> {

    List<SagaEvent> findBySagaIdOrderByCreatedAtAsc(String sagaId);
}
