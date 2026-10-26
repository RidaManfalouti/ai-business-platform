import { apiClient } from './client';
import { SpringPage } from '@/types/api';
import { CustomerRequest, CustomerResponse } from '@/types/customer';

export interface GetCustomersParams {
  page?: number;
  size?: number;
  sort?: string;
  name?: string;
  email?: string;
}

export const customersApi = {
  getCustomers: async (params: GetCustomersParams = {}): Promise<SpringPage<CustomerResponse>> => {
    // If search criteria provided, use /api/customers/search endpoint
    if (params.name?.trim() || params.email?.trim()) {
      const searchParams: Record<string, string | number> = {
        page: params.page ?? 0,
        size: params.size ?? 10,
        sort: params.sort ?? 'lastName,asc',
      };
      if (params.name?.trim()) searchParams.name = params.name.trim();
      if (params.email?.trim()) searchParams.email = params.email.trim();

      const response = await apiClient.get<SpringPage<CustomerResponse>>('/api/customers/search', {
        params: searchParams,
      });
      return response.data;
    }

    const queryParams: Record<string, string | number> = {
      page: params.page ?? 0,
      size: params.size ?? 10,
      sort: params.sort ?? 'id,asc',
    };
    const response = await apiClient.get<SpringPage<CustomerResponse>>('/api/customers', {
      params: queryParams,
    });
    return response.data;
  },

  getCustomerById: async (id: number): Promise<CustomerResponse> => {
    const response = await apiClient.get<CustomerResponse>(`/api/customers/${id}`);
    return response.data;
  },

  createCustomer: async (request: CustomerRequest): Promise<CustomerResponse> => {
    const response = await apiClient.post<CustomerResponse>('/api/customers', request);
    return response.data;
  },

  updateCustomer: async (id: number, request: CustomerRequest): Promise<CustomerResponse> => {
    const response = await apiClient.put<CustomerResponse>(`/api/customers/${id}`, request);
    return response.data;
  },

  deleteCustomer: async (id: number): Promise<void> => {
    await apiClient.delete(`/api/customers/${id}`);
  },
};
