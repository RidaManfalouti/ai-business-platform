import { apiClient } from './client';
import { SpringPage } from '@/types/api';
import {
  CreateSaleRequest,
  SaleResponse,
  UpdateSaleStatusRequest,
} from '@/types/sale';

export interface GetSalesParams {
  page?: number;
  size?: number;
  sort?: string;
}

export const salesApi = {
  getSales: async (params: GetSalesParams = {}): Promise<SpringPage<SaleResponse>> => {
    const queryParams: Record<string, string | number> = {
      page: params.page ?? 0,
      size: params.size ?? 10,
      sort: params.sort ?? 'createdAt,desc',
    };
    const response = await apiClient.get<SpringPage<SaleResponse>>('/api/sales', {
      params: queryParams,
    });
    return response.data;
  },

  getSaleById: async (id: number): Promise<SaleResponse> => {
    const response = await apiClient.get<SaleResponse>(`/api/sales/${id}`);
    return response.data;
  },

  getSalesByCustomer: async (
    customerId: number,
    params: GetSalesParams = {}
  ): Promise<SpringPage<SaleResponse>> => {
    const queryParams: Record<string, string | number> = {
      page: params.page ?? 0,
      size: params.size ?? 10,
      sort: params.sort ?? 'createdAt,desc',
    };
    const response = await apiClient.get<SpringPage<SaleResponse>>(
      `/api/sales/customer/${customerId}`,
      { params: queryParams }
    );
    return response.data;
  },

  createSale: async (
    request: CreateSaleRequest,
    idempotencyKey?: string
  ): Promise<SaleResponse> => {
    const headers: Record<string, string> = {};
    if (idempotencyKey && idempotencyKey.trim()) {
      headers['Idempotency-Key'] = idempotencyKey.trim();
    }
    const response = await apiClient.post<SaleResponse>('/api/sales', request, {
      headers,
    });
    return response.data;
  },

  updateSaleStatus: async (
    id: number,
    request: UpdateSaleStatusRequest
  ): Promise<SaleResponse> => {
    const response = await apiClient.patch<SaleResponse>(
      `/api/sales/${id}/status`,
      request
    );
    return response.data;
  },

  cancelSale: async (id: number): Promise<SaleResponse> => {
    const response = await apiClient.post<SaleResponse>(`/api/sales/${id}/cancel`);
    return response.data;
  },
};
