import { apiClient } from './client';
import {
  ReconciliationReport,
  Saga,
  SagaDetailsResponse,
  SagaStatus,
} from '@/types/saga';

export const sagasApi = {
  getSagas: async (status?: SagaStatus): Promise<Saga[]> => {
    const params: Record<string, string> = {};
    if (status) {
      params.status = status;
    }
    const response = await apiClient.get<Saga[]>('/api/sales/sagas', { params });
    return response.data;
  },

  getSagaDetails: async (sagaId: string): Promise<SagaDetailsResponse> => {
    const response = await apiClient.get<SagaDetailsResponse>(
      `/api/sales/sagas/${encodeURIComponent(sagaId)}`
    );
    return response.data;
  },

  retrySaga: async (sagaId: string): Promise<Saga> => {
    const response = await apiClient.post<Saga>(
      `/api/sales/sagas/${encodeURIComponent(sagaId)}/retry`
    );
    return response.data;
  },

  resolveSaga: async (sagaId: string, note?: string): Promise<Saga> => {
    const response = await apiClient.post<Saga>(
      `/api/sales/sagas/${encodeURIComponent(sagaId)}/resolve`,
      note ? { note } : {}
    );
    return response.data;
  },

  getLatestReconciliationReport: async (): Promise<ReconciliationReport> => {
    const response = await apiClient.get<ReconciliationReport>(
      '/api/sales/reconciliation/latest'
    );
    return response.data;
  },

  runReconciliation: async (staleMinutes = 15): Promise<ReconciliationReport> => {
    const response = await apiClient.post<ReconciliationReport>(
      '/api/sales/reconciliation/run',
      null,
      { params: { staleMinutes } }
    );
    return response.data;
  },
};
