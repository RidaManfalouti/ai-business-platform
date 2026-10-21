package com.aibi.sales_service.model;

import java.util.Set;

public enum SaleStatus {
    PENDING {
        @Override
        public Set<SaleStatus> allowedNextStatuses() {
            return Set.of(CONFIRMED, CANCELLED);
        }
    },
    CONFIRMED {
        @Override
        public Set<SaleStatus> allowedNextStatuses() {
            return Set.of(COMPLETED, CANCELLED);
        }
    },
    COMPLETED {
        @Override
        public Set<SaleStatus> allowedNextStatuses() {
            return Set.of();
        }
    },
    CANCELLED {
        @Override
        public Set<SaleStatus> allowedNextStatuses() {
            return Set.of();
        }
    };

    public abstract Set<SaleStatus> allowedNextStatuses();

    public boolean canTransitionTo(SaleStatus target) {
        if (target == null) {
            return false;
        }
        return allowedNextStatuses().contains(target);
    }
}
