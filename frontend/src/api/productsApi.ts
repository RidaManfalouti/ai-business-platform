import { apiClient } from './client';
import { SpringPage } from '@/types/api';
import {
  ProductRequest,
  ProductResponse,
  StockReservationResponse,
} from '@/types/product';

export interface GetProductsParams {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
}

export const productsApi = {
  getProducts: async (params: GetProductsParams = {}): Promise<SpringPage<ProductResponse>> => {
    const queryParams: Record<string, string | number> = {
      page: params.page ?? 0,
      size: params.size ?? 10,
      sort: params.sort ?? 'id,asc',
    };
    if (params.search && params.search.trim() !== '') {
      queryParams.search = params.search.trim();
    }
    const response = await apiClient.get<SpringPage<ProductResponse>>('/api/products', {
      params: queryParams,
    });
    return response.data;
  },

  getProductById: async (id: number): Promise<ProductResponse> => {
    const response = await apiClient.get<ProductResponse>(`/api/products/${id}`);
    return response.data;
  },

  createProduct: async (request: ProductRequest): Promise<ProductResponse> => {
    const response = await apiClient.post<ProductResponse>('/api/products', request);
    return response.data;
  },

  updateProduct: async (id: number, request: ProductRequest): Promise<ProductResponse> => {
    const response = await apiClient.put<ProductResponse>(`/api/products/${id}`, request);
    return response.data;
  },

  deleteProduct: async (id: number): Promise<void> => {
    await apiClient.delete(`/api/products/${id}`);
  },

  getStaleReservations: async (minutes = 30): Promise<StockReservationResponse[]> => {
    const response = await apiClient.get<StockReservationResponse[]>(
      '/api/products/reservations/stale',
      { params: { minutes } }
    );
    return response.data;
  },
};
