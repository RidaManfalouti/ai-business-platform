import { describe, it, expect, vi, beforeEach } from 'vitest';
import { analyticsApi } from './analyticsApi';
import { apiClient } from './client';

vi.mock('./client', () => ({
  apiClient: {
    get: vi.fn(),
  },
}));

describe('analyticsApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getDashboard appelle /api/analytics/dashboard avec les bons paramètres', async () => {
    const mockData = {
      kpi: {
        totalRevenue: 50000,
        completedSalesCount: 100,
        averageOrderValue: 500,
        totalSalesCount: 120,
        totalCustomers: 200,
        totalProducts: 45,
        currency: 'EUR',
        periodStart: '2026-10-01T00:00:00Z',
        periodEnd: '2026-10-27T23:59:59Z',
        timezone: 'UTC',
      },
      trends: { trends: [], totalPeriodRevenue: 50000, totalPeriodOrders: 100, currency: 'EUR' },
      topProducts: { items: [], totalQuantitySold: 0, totalRevenue: 0, currency: 'EUR' },
      statusDistribution: { distribution: [], totalOrders: 120 },
      lowStock: { items: [], lowStockCount: 0, threshold: 5 },
    };

    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: mockData });

    const result = await analyticsApi.getDashboard({ from: '2026-10-01', to: '2026-10-27', timezone: 'UTC' });

    expect(apiClient.get).toHaveBeenCalledWith('/api/analytics/dashboard', {
      params: { from: '2026-10-01', to: '2026-10-27', timezone: 'UTC' },
    });
    expect(result.kpi.totalRevenue).toBe(50000);
    expect(result.kpi.currency).toBe('EUR');
  });

  it('getKpi appelle /api/analytics/kpi', async () => {
    const mockKpi = {
      totalRevenue: 15000,
      completedSalesCount: 30,
      averageOrderValue: 500,
      totalSalesCount: 35,
      totalCustomers: 50,
      totalProducts: 20,
      currency: 'EUR',
      periodStart: '2026-10-01T00:00:00Z',
      periodEnd: '2026-10-27T23:59:59Z',
      timezone: 'UTC',
    };

    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: mockKpi });

    const result = await analyticsApi.getKpi({ from: '2026-10-01', to: '2026-10-27' });

    expect(apiClient.get).toHaveBeenCalledWith('/api/analytics/kpi', {
      params: { from: '2026-10-01', to: '2026-10-27' },
    });
    expect(result.totalRevenue).toBe(15000);
  });

  it('getLowStock appelle /api/analytics/low-stock avec threshold', async () => {
    const mockLowStock = {
      items: [
        {
          productId: 1,
          name: 'USB Key',
          category: 'Accessory',
          price: 10,
          stockQuantity: 2,
          stockStatus: 'LOW_STOCK' as const,
        },
      ],
      lowStockCount: 1,
      threshold: 5,
    };

    vi.mocked(apiClient.get).mockResolvedValueOnce({ data: mockLowStock });

    const result = await analyticsApi.getLowStock({ threshold: 5, limit: 10 });

    expect(apiClient.get).toHaveBeenCalledWith('/api/analytics/low-stock', {
      params: { threshold: 5, limit: 10 },
    });
    expect(result.items).toHaveLength(1);
    expect(result.items[0].stockStatus).toBe('LOW_STOCK');
  });
});
