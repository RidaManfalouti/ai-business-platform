import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { DashboardPage } from './DashboardPage';
import { analyticsApi } from '@/api/analyticsApi';
import { salesApi } from '@/api/salesApi';
import { SaleResponse } from '@/types/sale';
import { SpringPage } from '@/types/api';

vi.mock('@/api/analyticsApi', () => ({
  analyticsApi: {
    getDashboard: vi.fn(),
  },
}));

vi.mock('@/api/salesApi', () => ({
  salesApi: {
    getSales: vi.fn(),
  },
}));

describe('DashboardPage', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
  });

  const renderDashboard = () =>
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DashboardPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

  it('affiche le spinner de chargement initialement', () => {
    vi.mocked(analyticsApi.getDashboard).mockReturnValue(new Promise(() => {}));
    vi.mocked(salesApi.getSales).mockReturnValue(new Promise(() => {}));

    renderDashboard();

    expect(screen.getByText(/Calcul des métriques et agrégations SQL/i)).toBeInTheDocument();
  });

  it('affiche les indicateurs KPI et le graphique lors d’une réponse réussie', async () => {
    vi.mocked(analyticsApi.getDashboard).mockResolvedValueOnce({
      kpi: {
        totalRevenue: 42500,
        completedSalesCount: 85,
        averageOrderValue: 500,
        totalSalesCount: 100,
        totalCustomers: 150,
        totalProducts: 40,
        currency: 'EUR',
        periodStart: '2026-10-01T00:00:00Z',
        periodEnd: '2026-10-27T23:59:59Z',
        timezone: 'UTC',
      },
      trends: {
        trends: [
          { date: '2026-10-25', orderCount: 10, revenue: 5000, currency: 'EUR' },
          { date: '2026-10-26', orderCount: 15, revenue: 7500, currency: 'EUR' },
        ],
        totalPeriodRevenue: 12500,
        totalPeriodOrders: 25,
        currency: 'EUR',
      },
      topProducts: {
        items: [
          { productId: 1, productName: 'MacBook Pro', quantitySold: 20, revenue: 40000, currency: 'EUR' },
        ],
        totalQuantitySold: 20,
        totalRevenue: 40000,
        currency: 'EUR',
      },
      statusDistribution: {
        distribution: [
          { status: 'COMPLETED', count: 85, percentage: 85 },
          { status: 'CANCELLED', count: 15, percentage: 15 },
        ],
        totalOrders: 100,
      },
      lowStock: {
        items: [
          { productId: 2, name: 'Adaptateur USB-C', category: 'Accessoire', price: 29, stockQuantity: 2, stockStatus: 'LOW_STOCK' },
        ],
        lowStockCount: 1,
        threshold: 5,
      },
    });

    const mockSalesPage: SpringPage<SaleResponse> = {
      content: [
        {
          id: 1,
          reference: 'SAL-20261027-001',
          customerId: 10,
          status: 'COMPLETED',
          totalAmount: 2000,
          createdAt: '2026-10-27T10:00:00Z',
          updatedAt: null,
          items: [],
        },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 5,
      number: 0,
      numberOfElements: 1,
      first: true,
      last: true,
      empty: false,
    };

    vi.mocked(salesApi.getSales).mockResolvedValueOnce(mockSalesPage);

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText('Tableau de Bord Analytique')).toBeInTheDocument();
      expect(screen.getByText(/42\s*500/)).toBeInTheDocument();
      expect(screen.getByText('85')).toBeInTheDocument();
      expect(screen.getByText('150')).toBeInTheDocument();
      expect(screen.getByText('MacBook Pro')).toBeInTheDocument();
      expect(screen.getByText('Adaptateur USB-C')).toBeInTheDocument();
      expect(screen.getByText('SAL-20261027-001')).toBeInTheDocument();
    });
  });

  it('affiche un message d’erreur explicite si le service d’analytique est indisponible (sans fausses données)', async () => {
    vi.mocked(analyticsApi.getDashboard).mockRejectedValueOnce(
      new Error('Service Unavailable: 503')
    );
    const mockEmptyPage: SpringPage<SaleResponse> = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 5,
      number: 0,
      numberOfElements: 0,
      first: true,
      last: true,
      empty: true,
    };
    vi.mocked(salesApi.getSales).mockResolvedValueOnce(mockEmptyPage);

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText('Service d\'Analytique Indisponible')).toBeInTheDocument();
      expect(screen.getByText(/aucun chiffre fictif n'est présenté/i)).toBeInTheDocument();
    });
  });
});
