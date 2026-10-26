export type SagaStatus =
  | 'STARTED'
  | 'STOCK_RESERVED'
  | 'SALE_PERSISTED'
  | 'COMPLETED'
  | 'COMPENSATION_PENDING'
  | 'COMPENSATING'
  | 'COMPENSATED'
  | 'FAILED_REQUIRES_ATTENTION';

export interface Saga {
  id: number;
  sagaId: string;
  sagaType: string;
  saleId: number | null;
  saleReference: string;
  reservationId: string;
  status: SagaStatus;
  attemptCount: number;
  maxAttempts: number;
  createdAt: string;
  updatedAt: string | null;
  lastAttemptAt: string | null;
  nextRetryAt: string | null;
  completedAt: string | null;
  lastError: string | null;
  version: number;
}

export interface SagaEvent {
  id: number;
  sagaId: string;
  sagaStatus: SagaStatus;
  eventType: string;
  message: string;
  createdAt: string;
}

export interface SagaDetailsResponse {
  saga: Saga;
  events: SagaEvent[];
}

export interface ReconciliationDiscrepancy {
  discrepancyType: string;
  sagaId: string | null;
  saleReference: string | null;
  reservationId: string | null;
  description: string;
  detectedAt: string;
}

export interface ReconciliationReport {
  executedAt: string;
  sagasChecked: number;
  salesChecked: number;
  discrepanciesCount: number;
  discrepancies: ReconciliationDiscrepancy[];
}
