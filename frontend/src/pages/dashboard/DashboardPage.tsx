import React, { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { analyticsApi } from '@/api/analyticsApi';
import { salesApi } from '@/api/salesApi';
import { AnalyticsPeriod } from '@/types/analytics';
import { formatCurrency, formatDate, getSaleStatusBadge } from '@/utils/formatters';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { Alert } from '@/components/common/Alert';
import {
  Package,
  Users,
  ShoppingCart,
  TrendingUp,
  Plus,
  ArrowRight,
  CheckCircle,
  Clock,
  Ban,
  Activity,
  AlertTriangle,
  Calendar,
  RefreshCw,
  Award,
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const [period, setPeriod] = useState<AnalyticsPeriod>('30d');

  // Compute date range based on selected period
  const { fromDate, toDate, timezone } = useMemo(() => {
    const tz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
    const now = new Date();
    const to = now.toISOString().split('T')[0];

    const past = new Date(now);
    if (period === '7d') {
      past.setDate(now.getDate() - 7);
    } else if (period === '30d') {
      past.setDate(now.getDate() - 30);
    } else if (period === '90d') {
      past.setDate(now.getDate() - 90);
    } else {
      past.setDate(now.getDate() - 365);
    }
    const from = past.toISOString().split('T')[0];

    return { fromDate: from, toDate: to, timezone: tz };
  }, [period]);

  // Query Real Analytics Data from Analytics Service
  const {
    data: dashboardData,
    isLoading: loadingDashboard,
    isError: dashboardError,
    error: rawDashboardError,
    refetch: refetchDashboard,
  } = useQuery({
    queryKey: ['analytics', 'dashboard', period, fromDate, toDate],
    queryFn: () => analyticsApi.getDashboard({ from: fromDate, to: toDate, timezone }),
  });

  // Query Recent Orders directly from sales-service for live operational view
  const {
    data: recentSalesPage,
    isLoading: loadingRecentSales,
  } = useQuery({
    queryKey: ['dashboard', 'recent-sales'],
    queryFn: () => salesApi.getSales({ page: 0, size: 5, sort: 'createdAt,desc' }),
  });

  const kpi = dashboardData?.kpi;
  const trends = dashboardData?.trends;
  const topProducts = dashboardData?.topProducts;
  const statusDistribution = dashboardData?.statusDistribution;
  const lowStock = dashboardData?.lowStock;

  const recentSalesList = recentSalesPage?.content ?? [];

  // Determine maximum trend revenue for scaling SVG bars
  const maxTrendRevenue = useMemo(() => {
    if (!trends || trends.trends.length === 0) return 1;
    const max = Math.max(...trends.trends.map((t) => t.revenue));
    return max > 0 ? max : 1;
  }, [trends]);

  return (
    <div className="space-y-6">
      {/* Top Welcome & Quick Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-[#27272A]">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-[#FAFAFA] tracking-tight">
            Tableau de Bord Analytique
          </h1>
          <p className="text-xs sm:text-sm text-[#A1A1AA] mt-1">
            Indicateurs décisionnels certifiés en temps réel via le microservice d'analytique (port 8085)
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Link to="/sales">
            <Button variant="primary" size="sm" icon={<Plus className="w-4 h-4" />}>
              Nouvelle Vente
            </Button>
          </Link>
          <Link to="/products">
            <Button variant="outline" size="sm" icon={<Package className="w-4 h-4" />}>
              Catalogue
            </Button>
          </Link>
        </div>
      </div>

      {/* Period Filter & Timezone Bar */}
      <div className="bg-[#18181B] p-3 sm:p-4 rounded-2xl border border-[#27272A] shadow-xs flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="flex items-center gap-2 flex-wrap">
          <Calendar className="w-4 h-4 text-[#FACC15] shrink-0" />
          <span className="text-xs font-semibold text-[#A1A1AA]">Période d'analyse :</span>
          <div className="flex items-center gap-1 bg-[#202024] p-1 rounded-xl border border-[#27272A]">
            {(
              [
                { id: '7d', label: '7 jours' },
                { id: '30d', label: '30 jours' },
                { id: '90d', label: '90 jours' },
                { id: 'all', label: '1 an' },
              ] as const
            ).map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => setPeriod(item.id)}
                className={`px-3 py-1 text-xs font-medium rounded-lg transition-all ${
                  period === item.id
                    ? 'bg-[#FACC15] text-[#09090B] font-bold shadow-[0_0_12px_rgba(250,204,21,0.25)]'
                    : 'text-[#A1A1AA] hover:text-[#FAFAFA] hover:bg-[#27272E]'
                }`}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-2 text-xs text-[#A1A1AA]">
          <span className="font-mono bg-[#202024] px-2.5 py-1 rounded-lg border border-[#27272A] text-[#FAFAFA]">
            {fromDate} &rarr; {toDate}
          </span>
          <span className="text-[11px] text-[#71717A]">({timezone})</span>
          <button
            type="button"
            onClick={() => refetchDashboard()}
            className="p-1.5 hover:bg-[#202024] rounded-lg text-[#A1A1AA] hover:text-[#FAFAFA] transition-colors"
            title="Actualiser les données"
          >
            <RefreshCw className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Error state if Analytics Service is unavailable */}
      {dashboardError && (
        <Alert variant="error" title="Service d'Analytique Indisponible">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mt-1 text-xs">
            <span>
              Le microservice d’analytique n’a pas pu répondre ou est temporairement inaccessible.
              Les métriques consolidées ne peuvent pas être affichées pour garantir l'intégrité des données
              (aucun chiffre fictif n'est présenté).
              {rawDashboardError instanceof Error && (
                <span className="block mt-1 font-mono text-[11px] text-[#EF4444]">
                  {rawDashboardError.message}
                </span>
              )}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => refetchDashboard()}
              icon={<RefreshCw className="w-3.5 h-3.5" />}
            >
              Réessayer
            </Button>
          </div>
        </Alert>
      )}

      {loadingDashboard ? (
        <LoadingSpinner label="Calcul des métriques et agrégations SQL en direct..." />
      ) : kpi ? (
        <>
          {/* AI Intelligent Insights Banner */}
          <div className="rounded-2xl p-4 sm:p-5 border border-[#A855F7]/30 bg-gradient-to-r from-[#A855F7]/15 via-[#18181B] to-[#FACC15]/10 shadow-[0_0_30px_rgba(168,85,247,0.12)] flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div className="flex items-start gap-3.5">
              <div className="p-2.5 rounded-xl bg-[#A855F7]/20 text-[#A855F7] border border-[#A855F7]/40 shadow-[0_0_15px_rgba(168,85,247,0.3)] shrink-0">
                <TrendingUp className="w-5 h-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold uppercase tracking-wider text-[#A855F7]">
                    Insights IA Décisionnels
                  </span>
                  <Badge variant="purple" size="sm">
                    Temps Réel
                  </Badge>
                </div>
                <p className="text-xs sm:text-sm text-[#FAFAFA] mt-1 leading-relaxed">
                  Activité commerciale soutenue sur la période avec un panier moyen de <strong className="text-[#FAFAFA]">{formatCurrency(kpi.averageOrderValue)}</strong> ({kpi.completedSalesCount} commandes finalisées).{' '}
                  {lowStock && lowStock.lowStockCount > 0 ? (
                    <span className="text-[#EF4444] font-medium">
                      Attention : {lowStock.lowStockCount} référence(s) sont sous le seuil critique de stock.
                    </span>
                  ) : (
                    <span className="text-emerald-400 font-medium">
                      La chaîne logistique est nominale sans rupture critique.
                    </span>
                  )}
                </p>
              </div>
            </div>

            <Link to="/ai" className="shrink-0">
              <Button variant="ai" size="sm" icon={<Activity className="w-4 h-4" />}>
                Explorer avec l'IA Copilot
              </Button>
            </Link>
          </div>

          {/* KPI Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
            {/* KPI 1: Realized Revenue */}
            <div className="bg-[#18181B] p-5 rounded-2xl border border-[#27272A] shadow-xs hover:border-[#FACC15]/40 transition-all hover:scale-[1.01] hover:shadow-[0_0_20px_rgba(250,204,21,0.1)]">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-[#A1A1AA] uppercase tracking-wider">
                  Chiffre d'Affaires
                </span>
                <div className="p-2.5 bg-[#FACC15]/15 text-[#FACC15] rounded-xl border border-[#FACC15]/20">
                  <TrendingUp className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-3">
                <div className="text-2xl font-bold text-[#FACC15] tracking-tight drop-shadow-[0_0_10px_rgba(250,204,21,0.2)]">
                  {formatCurrency(kpi.totalRevenue)}
                </div>
                <div className="mt-1.5 flex items-center justify-between text-[11px] text-[#A1A1AA]">
                  <span>Ventes COMPLETED</span>
                  <span className="font-semibold text-[#FACC15]">Devise : {kpi.currency}</span>
                </div>
              </div>
            </div>

            {/* KPI 2: Completed Orders */}
            <div className="bg-[#18181B] p-5 rounded-2xl border border-[#27272A] shadow-xs hover:border-[#A855F7]/40 transition-all hover:scale-[1.01] hover:shadow-[0_0_20px_rgba(168,85,247,0.1)]">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-[#A1A1AA] uppercase tracking-wider">
                  Commandes Finalisées
                </span>
                <div className="p-2.5 bg-[#A855F7]/15 text-[#A855F7] rounded-xl border border-[#A855F7]/20">
                  <ShoppingCart className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-3">
                <div className="text-2xl font-bold text-[#FAFAFA] tracking-tight">
                  {kpi.completedSalesCount}
                </div>
                <div className="mt-1.5 text-[11px] text-[#71717A]">
                  sur {kpi.totalSalesCount} commandes enregistrées
                </div>
              </div>
            </div>

            {/* KPI 3: Average Order Value */}
            <div className="bg-[#18181B] p-5 rounded-2xl border border-[#27272A] shadow-xs hover:border-[#3F3F46] transition-all hover:scale-[1.01]">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-[#A1A1AA] uppercase tracking-wider">
                  Panier Moyen (AOV)
                </span>
                <div className="p-2.5 bg-[#202024] text-[#FACC15] rounded-xl border border-[#27272A]">
                  <Award className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-3">
                <div className="text-2xl font-bold text-[#FAFAFA] tracking-tight">
                  {formatCurrency(kpi.averageOrderValue)}
                </div>
                <div className="mt-1.5 text-[11px] text-[#71717A]">
                  Par commande finalisée
                </div>
              </div>
            </div>

            {/* KPI 4: Customers */}
            <div className="bg-[#18181B] p-5 rounded-2xl border border-[#27272A] shadow-xs hover:border-sky-500/40 transition-all hover:scale-[1.01]">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-[#A1A1AA] uppercase tracking-wider">
                  Clients CRM
                </span>
                <div className="p-2.5 bg-sky-500/15 text-sky-400 rounded-xl border border-sky-500/20">
                  <Users className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-3">
                <div className="text-2xl font-bold text-[#FAFAFA] tracking-tight">
                  {kpi.totalCustomers}
                </div>
                <div className="mt-1.5 flex items-center justify-between text-xs text-[#A1A1AA]">
                  <span>Portefeuille client</span>
                  <Link to="/customers" className="text-sky-400 font-semibold hover:underline">
                    Gérer &rarr;
                  </Link>
                </div>
              </div>
            </div>

            {/* KPI 5: Catalog Products */}
            <div className="bg-[#18181B] p-5 rounded-2xl border border-[#27272A] shadow-xs hover:border-teal-500/40 transition-all hover:scale-[1.01]">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-[#A1A1AA] uppercase tracking-wider">
                  Catalogue & Stock
                </span>
                <div className="p-2.5 bg-teal-500/15 text-teal-400 rounded-xl border border-teal-500/20">
                  <Package className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-3">
                <div className="text-2xl font-bold text-[#FAFAFA] tracking-tight">
                  {kpi.totalProducts}
                </div>
                <div className="mt-1.5 flex items-center justify-between text-xs text-[#A1A1AA]">
                  <span>Références actives</span>
                  <Link to="/products" className="text-teal-400 font-semibold hover:underline">
                    Détails &rarr;
                  </Link>
                </div>
              </div>
            </div>
          </div>

          {/* Trends & Status Distribution Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Daily Trends Chart (2 cols) */}
            <div className="lg:col-span-2 bg-[#18181B] rounded-2xl border border-[#27272A] shadow-xs p-5">
              <div className="flex items-center justify-between border-b border-[#27272A] pb-3 mb-4">
                <div className="flex items-center gap-2.5">
                  <div className="p-1.5 rounded-lg bg-[#FACC15]/15 text-[#FACC15]">
                    <TrendingUp className="w-4 h-4" />
                  </div>
                  <h3 className="text-sm font-bold text-[#FAFAFA]">
                    Évolution du Chiffre d'Affaires Réalisé
                  </h3>
                </div>
                <span className="text-xs font-semibold text-[#FACC15]">
                  Total : {formatCurrency(trends?.totalPeriodRevenue ?? 0)} ({trends?.totalPeriodOrders ?? 0} cmd)
                </span>
              </div>

              {!trends || trends.trends.length === 0 ? (
                <div className="py-12 text-center text-xs text-[#71717A]">
                  Aucune vente finalisée (COMPLETED) enregistrée sur la période sélectionnée ({fromDate} à {toDate}).
                </div>
              ) : (
                <div className="space-y-4">
                  {/* Visual SVG Timeline Chart */}
                  <div className="h-44 w-full flex items-end gap-1.5 pt-6 pb-2 px-1">
                    {trends.trends.map((pt) => {
                      const heightPercent = Math.max(8, Math.round((pt.revenue / maxTrendRevenue) * 100));
                      return (
                        <div
                          key={pt.date}
                          className="flex-1 flex flex-col items-center group relative h-full justify-end"
                        >
                          {/* Tooltip on hover */}
                          <div className="absolute -top-10 opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity bg-[#202024] text-[#FAFAFA] border border-[#27272A] text-[10px] px-2.5 py-1 rounded-lg shadow-xl z-10 whitespace-nowrap">
                            <span className="font-semibold text-[#FACC15]">{pt.date}</span> : {formatCurrency(pt.revenue)} ({pt.orderCount} cmd)
                          </div>
                          {/* Bar */}
                          <div
                            style={{ height: `${heightPercent}%` }}
                            className="w-full bg-[#FACC15] hover:bg-[#EAB308] rounded-t-md transition-all shadow-[0_0_10px_rgba(250,204,21,0.2)]"
                          />
                          {/* Date label */}
                          <span className="text-[9px] text-[#71717A] mt-1.5 truncate max-w-full font-mono">
                            {pt.date.slice(5)}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                  <div className="flex items-center justify-between text-[11px] text-[#71717A] border-t border-[#27272A] pt-2.5">
                    <span>* Calculé strictement sur les commandes au statut COMPLETED</span>
                    <span className="text-[#A1A1AA]">Montants en {kpi.currency}</span>
                  </div>
                </div>
              )}
            </div>

            {/* Sales Status Breakdown (1 col) */}
            <div className="bg-[#18181B] rounded-2xl border border-[#27272A] shadow-xs p-5 flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between border-b border-[#27272A] pb-3 mb-4">
                  <h3 className="text-sm font-bold text-[#FAFAFA]">
                    Distribution des Statuts
                  </h3>
                  <span className="text-xs text-[#71717A]">
                    {statusDistribution?.totalOrders ?? 0} commandes
                  </span>
                </div>

                <div className="space-y-3.5">
                  {statusDistribution?.distribution.map((item) => {
                    let progressColor = 'bg-[#71717A]';
                    let IconComponent = Clock;

                    if (item.status === 'COMPLETED') {
                      progressColor = 'bg-emerald-500 shadow-[0_0_10px_rgba(34,197,94,0.3)]';
                      IconComponent = CheckCircle;
                    } else if (item.status === 'CONFIRMED') {
                      progressColor = 'bg-sky-500 shadow-[0_0_10px_rgba(56,189,248,0.3)]';
                      IconComponent = CheckCircle;
                    } else if (item.status === 'PENDING') {
                      progressColor = 'bg-[#FACC15] shadow-[0_0_10px_rgba(250,204,21,0.3)]';
                      IconComponent = Clock;
                    } else if (item.status === 'CANCELLED') {
                      progressColor = 'bg-[#EF4444] shadow-[0_0_10px_rgba(239,68,68,0.3)]';
                      IconComponent = Ban;
                    }

                    return (
                      <div key={item.status} className="space-y-1.5">
                        <div className="flex items-center justify-between text-xs">
                          <span className="flex items-center gap-1.5 font-medium text-[#A1A1AA]">
                            <IconComponent className="w-3.5 h-3.5" />
                            {item.status}
                          </span>
                          <span className="font-semibold text-[#FAFAFA]">
                            {item.count} ({item.percentage}%)
                          </span>
                        </div>
                        <div className="w-full h-2 bg-[#202024] rounded-full overflow-hidden">
                          <div
                            className={`h-full ${progressColor} transition-all duration-300`}
                            style={{ width: `${Math.min(100, item.percentage)}%` }}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              <div className="mt-5 pt-3 border-t border-[#27272A] text-[11px] text-[#71717A]">
                Seules les commandes <strong>COMPLETED</strong> contribuent au Chiffre d'Affaires réalisé.
              </div>
            </div>
          </div>

          {/* Top Selling Products & Low Stock Alerts */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Top Products */}
            <div className="bg-[#18181B] rounded-2xl border border-[#27272A] shadow-xs overflow-hidden">
              <div className="p-4 sm:p-5 border-b border-[#27272A] flex items-center justify-between bg-[#202024]/30">
                <div className="flex items-center gap-2">
                  <div className="p-1.5 rounded-lg bg-[#A855F7]/15 text-[#A855F7]">
                    <Award className="w-4 h-4" />
                  </div>
                  <h3 className="text-sm font-bold text-[#FAFAFA]">
                    Meilleures Ventes (Top Produits)
                  </h3>
                </div>
                <span className="text-xs text-[#71717A]">
                  {topProducts?.totalQuantitySold ?? 0} unités vendues
                </span>
              </div>

              {!topProducts || topProducts.items.length === 0 ? (
                <div className="p-8 text-center text-xs text-[#71717A]">
                  Aucun article vendu sur cette période.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs text-[#A1A1AA] divide-y divide-[#27272A]">
                    <thead className="bg-[#131316] text-[11px] font-semibold uppercase text-[#A1A1AA]">
                      <tr>
                        <th className="px-4 py-3">Produit</th>
                        <th className="px-4 py-3 text-right">Quantité Vendue</th>
                        <th className="px-4 py-3 text-right">CA Réalisé</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#27272A]">
                      {topProducts.items.map((prod) => (
                        <tr key={prod.productId} className="hover:bg-[#202024]/60 transition-colors">
                          <td className="px-4 py-3 font-medium text-[#FAFAFA]">
                            {prod.productName}
                          </td>
                          <td className="px-4 py-3 text-right font-mono font-semibold text-[#FAFAFA]">
                            {prod.quantitySold}
                          </td>
                          <td className="px-4 py-3 text-right font-semibold text-[#FACC15]">
                            {formatCurrency(prod.revenue)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Low Stock Alerts */}
            <div className="bg-[#18181B] rounded-2xl border border-[#27272A] shadow-xs overflow-hidden">
              <div className="p-4 sm:p-5 border-b border-[#27272A] flex items-center justify-between bg-[#202024]/30">
                <div className="flex items-center gap-2">
                  <div className="p-1.5 rounded-lg bg-[#EF4444]/15 text-[#EF4444]">
                    <AlertTriangle className="w-4 h-4" />
                  </div>
                  <h3 className="text-sm font-bold text-[#FAFAFA]">
                    Alertes Inventaire (Stock Faible & Rupture)
                  </h3>
                </div>
                <Badge variant="warning" size="sm">
                  Seuil : &le; {lowStock?.threshold ?? 5} unités
                </Badge>
              </div>

              {!lowStock || lowStock.items.length === 0 ? (
                <div className="p-8 text-center text-xs text-emerald-400 flex items-center justify-center gap-2">
                  <CheckCircle className="w-4 h-4" />
                  <span>Tous les stocks sont au-dessus du seuil d'alerte critique.</span>
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs text-[#A1A1AA] divide-y divide-[#27272A]">
                    <thead className="bg-[#131316] text-[11px] font-semibold uppercase text-[#A1A1AA]">
                      <tr>
                        <th className="px-4 py-3">Produit</th>
                        <th className="px-4 py-3">Statut</th>
                        <th className="px-4 py-3 text-right">Stock Actuel</th>
                        <th className="px-4 py-3 text-right">Prix Unitaire</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#27272A]">
                      {lowStock.items.map((item) => (
                        <tr key={item.productId} className="hover:bg-[#202024]/60 transition-colors">
                          <td className="px-4 py-3 font-medium text-[#FAFAFA]">
                            <Link to="/products" className="hover:underline text-[#FAFAFA]">
                              {item.name}
                            </Link>
                          </td>
                          <td className="px-4 py-3">
                            <Badge
                              variant={item.stockStatus === 'OUT_OF_STOCK' ? 'danger' : 'warning'}
                              size="sm"
                            >
                              {item.stockStatus === 'OUT_OF_STOCK' ? 'Rupture' : 'Stock faible'}
                            </Badge>
                          </td>
                          <td className="px-4 py-3 text-right font-mono font-bold text-[#FAFAFA]">
                            {item.stockQuantity}
                          </td>
                          <td className="px-4 py-3 text-right text-[#A1A1AA]">
                            {formatCurrency(item.price)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>

          {/* Recent Orders Section */}
          <div className="bg-[#18181B] rounded-2xl border border-[#27272A] shadow-xs overflow-hidden">
            <div className="p-4 sm:p-5 border-b border-[#27272A] flex items-center justify-between bg-[#202024]/30">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-[#FACC15]/15 text-[#FACC15]">
                  <Activity className="w-4 h-4" />
                </div>
                <h3 className="text-sm font-bold text-[#FAFAFA]">Dernières Commandes Passées</h3>
              </div>
              <Link
                to="/sales"
                className="text-xs font-semibold text-[#FACC15] hover:text-[#EAB308] flex items-center gap-1 transition-colors"
              >
                <span>Voir l'historique complet des ventes</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>

            {loadingRecentSales ? (
              <div className="p-6">
                <LoadingSpinner label="Chargement des commandes récentes..." />
              </div>
            ) : recentSalesList.length === 0 ? (
              <div className="p-8 text-center text-xs text-[#71717A]">
                Aucune vente enregistrée pour le moment.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-[#A1A1AA] divide-y divide-[#27272A]">
                  <thead className="bg-[#131316] text-[11px] font-semibold uppercase text-[#A1A1AA]">
                    <tr>
                      <th className="px-4 py-3">Réf. Vente</th>
                      <th className="px-4 py-3">Statut</th>
                      <th className="px-4 py-3">Montant Total</th>
                      <th className="px-4 py-3">Articles</th>
                      <th className="px-4 py-3">Date</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#27272A]">
                    {recentSalesList.map((sale) => {
                      const statusBadge = getSaleStatusBadge(sale.status);
                      return (
                        <tr key={sale.id} className="hover:bg-[#202024]/60 transition-colors">
                          <td className="px-4 py-3 font-mono font-semibold text-[#FAFAFA]">
                            <Link to={`/sales/${sale.id}`} className="hover:underline text-[#FACC15]">
                              {sale.reference}
                            </Link>
                          </td>
                          <td className="px-4 py-3">
                            <Badge variant={statusBadge.variant} size="sm">
                              {statusBadge.label}
                            </Badge>
                          </td>
                          <td className="px-4 py-3 font-semibold text-[#FAFAFA]">
                            {formatCurrency(sale.totalAmount)}
                          </td>
                          <td className="px-4 py-3 text-[#A1A1AA]">
                            {sale.items?.length || 0} référence(s)
                          </td>
                          <td className="px-4 py-3 text-[#71717A]">
                            {formatDate(sale.createdAt)}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      ) : null}
    </div>
  );
};
