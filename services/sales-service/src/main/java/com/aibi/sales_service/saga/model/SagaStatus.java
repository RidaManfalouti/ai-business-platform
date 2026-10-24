package com.aibi.sales_service.saga.model;

import java.util.Set;

public enum SagaStatus {
    STARTED {
        @Override
        public Set<SagaStatus> allowedNextStatuses() {
            return Set.of(STOCK_RESERVED, COMPENSATION_PENDING, COMPENSATED);
        }
    },
    STOCK_RESERVED {
        @Override
        public Set<SagaStatus> allowedNextStatuses() {
            return Set.of(SALE_PERSISTED, COMPENSATION_PENDING, COMPENSATING);
        }
    },
    SALE_PERSISTED {
        @Override
        public Set<SagaStatus> allowedNextStatuses() {
            return Set.of(COMPLETED, COMPENSATION_PENDING);
        }
    },
    COMPLETED {
        @Override
        public Set<SagaStatus> allowedNextStatuses() {
            // Allows transition to compensation when a completed sale is cancelled
            return Set.of(COMPENSATION_PENDING);
        }
    },
    COMPENSATION_PENDING {
        @Override
        public Set<SagaStatus> allowedNextStatuses() {
            return Set.of(COMPENSATING, FAILED_REQUIRES_ATTENTION, COMPENSATED);
        }
    },
    COMPENSATING {
        @Override
        public Set<SagaStatus> allowedNextStatuses() {
            return Set.of(COMPENSATED, COMPENSATION_PENDING, FAILED_REQUIRES_ATTENTION);
        }
    },
    COMPENSATED {
        @Override
        public Set<SagaStatus> allowedNextStatuses() {
            return Set.of();
        }
    },
    FAILED_REQUIRES_ATTENTION {
        @Override
        public Set<SagaStatus> allowedNextStatuses() {
            // Allows manual retry (COMPENSATION_PENDING) or administrative resolution (COMPENSATED)
            return Set.of(COMPENSATION_PENDING, COMPENSATED);
        }
    };

    public abstract Set<SagaStatus> allowedNextStatuses();

    public boolean canTransitionTo(SagaStatus target) {
        if (target == null) {
            return false;
        }
        return allowedNextStatuses().contains(target);
    }
}
