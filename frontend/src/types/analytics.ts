export type AnalyticsPeriod = '7d' | '30d' | '90d' | 'all';

export interface KpiSummary {
  totalRevenue: number;
  completedSalesCount: number;
  averageOrderValue: number;
  totalSalesCount: number;
  totalCustomers: number;
  totalProducts: number;
  currency: string;
  periodStart: string;
  periodEnd: string;
  timezone: string;
}

export interface RevenueTrendPoint {
  date: string;
  orderCount: number;
  revenue: number;
  currency: string;
}

export interface TrendsData {
  trends: RevenueTrendPoint[];
  totalPeriodRevenue: number;
  totalPeriodOrders: number;
  currency: string;
}

export interface TopProduct {
  productId: number;
  productName: string;
  quantitySold: number;
  revenue: number;
  currency: string;
}

export interface TopProductsData {
  items: TopProduct[];
  totalQuantitySold: number;
  totalRevenue: number;
  currency: string;
}

export interface StatusDistributionItem {
  status: string;
  count: number;
  percentage: number;
}

export interface StatusDistributionData {
  distribution: StatusDistributionItem[];
  totalOrders: number;
}

export interface LowStockItem {
  productId: number;
  name: string;
  category: string;
  price: number;
  stockQuantity: number;
  stockStatus: 'OUT_OF_STOCK' | 'LOW_STOCK';
}

export interface LowStockData {
  items: LowStockItem[];
  lowStockCount: number;
  threshold: number;
}

export interface DashboardAnalytics {
  kpi: KpiSummary;
  trends: TrendsData;
  topProducts: TopProductsData;
  statusDistribution: StatusDistributionData;
  lowStock: LowStockData;
}
