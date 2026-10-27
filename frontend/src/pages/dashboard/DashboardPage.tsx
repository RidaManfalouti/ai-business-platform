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
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
            Tableau de Bord Analytique
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
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
      <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="flex items-center gap-2">
          <Calendar className="w-4 h-4 text-emerald-600 shrink-0" />
          <span className="text-xs font-semibold text-slate-700">Période d'analyse :</span>
          <div className="flex items-center gap-1 bg-slate-100 p-0.5 rounded-lg">
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
                className={`px-2.5 py-1 text-xs font-medium rounded-md transition-all ${
                  period === item.id
                    ? 'bg-white text-emerald-700 shadow-xs font-semibold'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-2 text-xs text-slate-500">
          <span className="font-mono bg-slate-50 px-2 py-0.5 rounded border border-slate-200">
            {fromDate} &rarr; {toDate}
          </span>
          <span className="text-[11px] text-slate-400">({timezone})</span>
          <button
            type="button"
            onClick={() => refetchDashboard()}
            className="p-1 hover:bg-slate-100 rounded text-slate-500 transition-colors"
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
                <span className="block mt-1 font-mono text-[11px] text-rose-700">
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
          {/* KPI Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
            {/* KPI 1: Realized Revenue */}
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition-colors">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Chiffre d'Affaires
                </span>
                <div className="p-2 bg-emerald-50 text-emerald-600 rounded-lg">
                  <TrendingUp className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-3">
                <div className="text-2xl font-bold text-slate-900">
                  {formatCurrency(kpi.totalRevenue)}
                </div>
                <div className="mt-1 flex items-center justify-between text-[11px] text-slate-500">
                  <span>Ventes COMPLETED</span>
                  <span className="font-semibold text-emerald-600">Devise : {kpi.currency}</span>
                </div>
              </div>
            </div>

            {/* KPI 2: Completed Orders */}
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition-colors">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Commandes Finalisées
                </span>
                <div className="p-2 bg-purple-50 text-purple-600 rounded-lg">
                  <ShoppingCart className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-3">
                <div className="text-2xl font-bold text-slate-900">
                  {kpi.completedSalesCount}
                </div>
                <div className="mt-1 text-[11px] text-slate-500">
                  sur {kpi.totalSalesCount} commandes enregistrées
                </div>
              </div>
            </div>

            {/* KPI 3: Average Order Value */}
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition-colors">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Panier Moyen (AOV)
                </span>
                <div className="p-2 bg-amber-50 text-amber-600 rounded-lg">
                  <Award className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-3">
                <div className="text-2xl font-bold text-slate-900">
                  {formatCurrency(kpi.averageOrderValue)}
                </div>
                <div className="mt-1 text-[11px] text-slate-400">
                  Par commande finalisée
                </div>
              </div>
            </div>

            {/* KPI 4: Customers */}
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition-colors">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Clients CRM
                </span>
                <div className="p-2 bg-sky-50 text-sky-600 rounded-lg">
                  <Users className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-3">
                <div className="text-2xl font-bold text-slate-900">
                  {kpi.totalCustomers}
                </div>
                <div className="mt-1 flex items-center justify-between text-xs text-slate-500">
                  <span>Portefeuille client</span>
                  <Link to="/customers" className="text-sky-600 font-semibold hover:underline">
                    Gérer &rarr;
                  </Link>
                </div>
              </div>
            </div>

            {/* KPI 5: Catalog Products */}
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition-colors">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Catalogue & Stock
                </span>
                <div className="p-2 bg-teal-50 text-teal-600 rounded-lg">
                  <Package className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-3">
                <div className="text-2xl font-bold text-slate-900">
                  {kpi.totalProducts}
                </div>
                <div className="mt-1 flex items-center justify-between text-xs text-slate-500">
                  <span>Références actives</span>
                  <Link to="/products" className="text-teal-600 font-semibold hover:underline">
                    Détails &rarr;
                  </Link>
                </div>
              </div>
            </div>
          </div>

          {/* Trends & Status Distribution Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Daily Trends Chart (2 cols) */}
            <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 shadow-xs p-5">
              <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
                <div className="flex items-center gap-2">
                  <TrendingUp className="w-4 h-4 text-emerald-600" />
                  <h3 className="text-sm font-bold text-slate-900">
                    Évolution du Chiffre d'Affaires Réalisé
                  </h3>
                </div>
                <span className="text-xs font-semibold text-slate-500">
                  Total : {formatCurrency(trends?.totalPeriodRevenue ?? 0)} ({trends?.totalPeriodOrders ?? 0} cmd)
                </span>
              </div>

              {!trends || trends.trends.length === 0 ? (
                <div className="py-12 text-center text-xs text-slate-400">
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
                          <div className="absolute -top-10 opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity bg-slate-900 text-white text-[10px] px-2 py-1 rounded shadow-md z-10 whitespace-nowrap">
                            <span className="font-semibold">{pt.date}</span> : {formatCurrency(pt.revenue)} ({pt.orderCount} cmd)
                          </div>
                          {/* Bar */}
                          <div
                            style={{ height: `${heightPercent}%` }}
                            className="w-full bg-emerald-500 hover:bg-emerald-600 rounded-t-sm transition-all"
                          />
                          {/* Date label */}
                          <span className="text-[9px] text-slate-400 mt-1 truncate max-w-full">
                            {pt.date.slice(5)}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                  <div className="flex items-center justify-between text-[11px] text-slate-400 border-t border-slate-100 pt-2">
                    <span>* Calculé strictement sur les commandes au statut COMPLETED</span>
                    <span>Montants en {kpi.currency}</span>
                  </div>
                </div>
              )}
            </div>

            {/* Sales Status Breakdown (1 col) */}
            <div className="bg-white rounded-xl border border-slate-200 shadow-xs p-5 flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
                  <h3 className="text-sm font-bold text-slate-900">
                    Distribution des Statuts
                  </h3>
                  <span className="text-xs text-slate-400">
                    {statusDistribution?.totalOrders ?? 0} commandes
                  </span>
                </div>

                <div className="space-y-3.5">
                  {statusDistribution?.distribution.map((item) => {
                    let progressColor = 'bg-slate-400';
                    let IconComponent = Clock;

                    if (item.status === 'COMPLETED') {
                      progressColor = 'bg-emerald-500';
                      IconComponent = CheckCircle;
                    } else if (item.status === 'CONFIRMED') {
                      progressColor = 'bg-sky-500';
                      IconComponent = CheckCircle;
                    } else if (item.status === 'PENDING') {
                      progressColor = 'bg-amber-500';
                      IconComponent = Clock;
                    } else if (item.status === 'CANCELLED') {
                      progressColor = 'bg-rose-500';
                      IconComponent = Ban;
                    }

                    return (
                      <div key={item.status} className="space-y-1">
                        <div className="flex items-center justify-between text-xs">
                          <span className="flex items-center gap-1.5 font-medium text-slate-700">
                            <IconComponent className="w-3.5 h-3.5" />
                            {item.status}
                          </span>
                          <span className="font-semibold text-slate-900">
                            {item.count} ({item.percentage}%)
                          </span>
                        </div>
                        <div className="w-full h-2 bg-slate-100 rounded-full overflow-hidden">
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

              <div className="mt-5 pt-3 border-t border-slate-100 text-[11px] text-slate-400">
                Seules les commandes <strong>COMPLETED</strong> contribuent au Chiffre d'Affaires réalisé.
              </div>
            </div>
          </div>

          {/* Top Selling Products & Low Stock Alerts */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Top Products */}
            <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
              <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Award className="w-4 h-4 text-emerald-600" />
                  <h3 className="text-sm font-bold text-slate-900">
                    Meilleures Ventes (Top Produits)
                  </h3>
                </div>
                <span className="text-xs text-slate-400">
                  {topProducts?.totalQuantitySold ?? 0} unités vendues
                </span>
              </div>

              {!topProducts || topProducts.items.length === 0 ? (
                <div className="p-8 text-center text-xs text-slate-400">
                  Aucun article vendu sur cette période.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs text-slate-600 divide-y divide-slate-100">
                    <thead className="bg-slate-50/70 text-[11px] font-semibold uppercase text-slate-500">
                      <tr>
                        <th className="px-4 py-3">Produit</th>
                        <th className="px-4 py-3 text-right">Quantité Vendue</th>
                        <th className="px-4 py-3 text-right">CA Réalisé</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {topProducts.items.map((prod) => (
                        <tr key={prod.productId} className="hover:bg-slate-50/50 transition-colors">
                          <td className="px-4 py-3 font-medium text-slate-900">
                            {prod.productName}
                          </td>
                          <td className="px-4 py-3 text-right font-mono font-semibold text-slate-800">
                            {prod.quantitySold}
                          </td>
                          <td className="px-4 py-3 text-right font-semibold text-emerald-600">
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
            <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
              <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 text-amber-500" />
                  <h3 className="text-sm font-bold text-slate-900">
                    Alertes Inventaire (Stock Faible & Rupture)
                  </h3>
                </div>
                <Badge variant="warning" size="sm">
                  Seuil : &le; {lowStock?.threshold ?? 5} unités
                </Badge>
              </div>

              {!lowStock || lowStock.items.length === 0 ? (
                <div className="p-8 text-center text-xs text-emerald-600 flex items-center justify-center gap-2">
                  <CheckCircle className="w-4 h-4" />
                  <span>Tous les stocks sont au-dessus du seuil d'alerte critique.</span>
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs text-slate-600 divide-y divide-slate-100">
                    <thead className="bg-slate-50/70 text-[11px] font-semibold uppercase text-slate-500">
                      <tr>
                        <th className="px-4 py-3">Produit</th>
                        <th className="px-4 py-3">Statut</th>
                        <th className="px-4 py-3 text-right">Stock Actuel</th>
                        <th className="px-4 py-3 text-right">Prix Unitaire</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {lowStock.items.map((item) => (
                        <tr key={item.productId} className="hover:bg-slate-50/50 transition-colors">
                          <td className="px-4 py-3 font-medium text-slate-900">
                            <Link to="/products" className="hover:underline text-slate-900">
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
                          <td className="px-4 py-3 text-right font-mono font-bold text-slate-900">
                            {item.stockQuantity}
                          </td>
                          <td className="px-4 py-3 text-right text-slate-600">
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
          <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
            <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Activity className="w-4 h-4 text-emerald-600" />
                <h3 className="text-sm font-bold text-slate-900">Dernières Commandes Passées</h3>
              </div>
              <Link
                to="/sales"
                className="text-xs font-semibold text-emerald-600 hover:text-emerald-700 flex items-center gap-1"
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
              <div className="p-8 text-center text-xs text-slate-400">
                Aucune vente enregistrée pour le moment.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-slate-600 divide-y divide-slate-100">
                  <thead className="bg-slate-50/70 text-[11px] font-semibold uppercase text-slate-500">
                    <tr>
                      <th className="px-4 py-3">Réf. Vente</th>
                      <th className="px-4 py-3">Statut</th>
                      <th className="px-4 py-3">Montant Total</th>
                      <th className="px-4 py-3">Articles</th>
                      <th className="px-4 py-3">Date</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {recentSalesList.map((sale) => {
                      const statusBadge = getSaleStatusBadge(sale.status);
                      return (
                        <tr key={sale.id} className="hover:bg-slate-50/50 transition-colors">
                          <td className="px-4 py-3 font-mono font-semibold text-slate-900">
                            <Link to={`/sales/${sale.id}`} className="hover:underline text-emerald-700">
                              {sale.reference}
                            </Link>
                          </td>
                          <td className="px-4 py-3">
                            <Badge variant={statusBadge.variant} size="sm">
                              {statusBadge.label}
                            </Badge>
                          </td>
                          <td className="px-4 py-3 font-semibold text-slate-900">
                            {formatCurrency(sale.totalAmount)}
                          </td>
                          <td className="px-4 py-3 text-slate-500">
                            {sale.items?.length || 0} référence(s)
                          </td>
                          <td className="px-4 py-3 text-slate-400">
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
