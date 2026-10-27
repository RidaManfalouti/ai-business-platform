import { apiClient } from './client';
import {
  DashboardAnalytics,
  KpiSummary,
  TrendsData,
  TopProductsData,
  StatusDistributionData,
  LowStockData,
} from '@/types/analytics';

export interface DateRangeParams {
  from?: string;
  to?: string;
  timezone?: string;
}

export const analyticsApi = {
  getDashboard: async (params?: DateRangeParams & { topLimit?: number; lowStockThreshold?: number }): Promise<DashboardAnalytics> => {
    const response = await apiClient.get<DashboardAnalytics>('/api/analytics/dashboard', { params });
    return response.data;
  },

  getKpi: async (params?: DateRangeParams): Promise<KpiSummary> => {
    const response = await apiClient.get<KpiSummary>('/api/analytics/kpi', { params });
    return response.data;
  },

  getTrends: async (params?: DateRangeParams): Promise<TrendsData> => {
    const response = await apiClient.get<TrendsData>('/api/analytics/trends', { params });
    return response.data;
  },

  getTopProducts: async (params?: DateRangeParams & { limit?: number }): Promise<TopProductsData> => {
    const response = await apiClient.get<TopProductsData>('/api/analytics/top-products', { params });
    return response.data;
  },

  getStatusDistribution: async (params?: DateRangeParams): Promise<StatusDistributionData> => {
    const response = await apiClient.get<StatusDistributionData>('/api/analytics/status-distribution', { params });
    return response.data;
  },

  getLowStock: async (params?: { threshold?: number; limit?: number }): Promise<LowStockData> => {
    const response = await apiClient.get<LowStockData>('/api/analytics/low-stock', { params });
    return response.data;
  },
};
