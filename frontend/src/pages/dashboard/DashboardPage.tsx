import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { productsApi } from '@/api/productsApi';
import { customersApi } from '@/api/customersApi';
import { salesApi } from '@/api/salesApi';
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
  Info,
  CheckCircle,
  Clock,
  Ban,
  Activity,
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  // Query product metrics
  const {
    data: productsPage,
    isLoading: loadingProducts,
    error: productsError,
  } = useQuery({
    queryKey: ['dashboard', 'products'],
    queryFn: () => productsApi.getProducts({ page: 0, size: 10, sort: 'id,desc' }),
  });

  // Query customer metrics
  const {
    data: customersPage,
    isLoading: loadingCustomers,
    error: customersError,
  } = useQuery({
    queryKey: ['dashboard', 'customers'],
    queryFn: () => customersApi.getCustomers({ page: 0, size: 10, sort: 'id,desc' }),
  });

  // Query sales metrics and recent orders
  const {
    data: salesPage,
    isLoading: loadingSales,
    error: salesError,
  } = useQuery({
    queryKey: ['dashboard', 'sales'],
    queryFn: () => salesApi.getSales({ page: 0, size: 5, sort: 'createdAt,desc' }),
  });

  const isLoading = loadingProducts || loadingCustomers || loadingSales;

  // Real calculations strictly from fetched backend data
  const totalProducts = productsPage?.totalElements ?? 0;
  const totalCustomers = customersPage?.totalElements ?? 0;
  const totalSales = salesPage?.totalElements ?? 0;

  // Calculate recent volume from latest fetched sales
  const recentSalesList = salesPage?.content ?? [];
  const recentSalesAmount = recentSalesList.reduce((acc, sale) => acc + (sale.totalAmount || 0), 0);

  // Status breakdown of recent sales
  const pendingCount = recentSalesList.filter((s) => s.status === 'PENDING').length;
  const confirmedCount = recentSalesList.filter((s) => s.status === 'CONFIRMED').length;
  const completedCount = recentSalesList.filter((s) => s.status === 'COMPLETED').length;
  const cancelledCount = recentSalesList.filter((s) => s.status === 'CANCELLED').length;

  return (
    <div className="space-y-6">
      {/* Top Welcome & Quick Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
            Vue d'ensemble de la plateforme
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Supervision opérationnelle en direct via la passerelle API Gateway (port 8080)
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

      {/* Analytics Service Fallback Notice */}
      <Alert variant="info" title="Microservice d'Analytique & IA (/api/analytics/**)">
        <div className="flex items-start gap-2.5 mt-1 text-xs">
          <Info className="w-4 h-4 text-sky-600 shrink-0 mt-0.5" />
          <span>
            Le service analytique prédictif n’est pas encore déployé (réponse HTTP 503 contrôlée par la
            passerelle). Les indicateurs ci-dessous reflètent exclusivement les données réelles et vérifiées
            fournies par <code>product-service</code>, <code>customer-service</code> et <code>sales-service</code>.
          </span>
        </div>
      </Alert>

      {/* Error alert if any core service fails */}
      {(productsError || customersError || salesError) && (
        <Alert variant="error" title="Avertissement de synchronisation">
          Une ou plusieurs requêtes n'ont pas pu aboutir. Vérifiez que les microservices sous-jacents sont
          démarrés et accessibles via la passerelle.
        </Alert>
      )}

      {isLoading ? (
        <LoadingSpinner label="Calcul des métriques en direct..." />
      ) : (
        <>
          {/* KPI Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* KPI: Products */}
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition-colors">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Produits au catalogue
                </span>
                <div className="p-2 bg-emerald-50 text-emerald-600 rounded-lg">
                  <Package className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-3">
                <div className="text-2xl font-bold text-slate-900">{totalProducts}</div>
                <div className="mt-1 flex items-center justify-between text-xs text-slate-500">
                  <span>Stock total vérifié</span>
                  <Link to="/products" className="text-emerald-600 font-semibold hover:underline">
                    Gérer &rarr;
                  </Link>
                </div>
              </div>
            </div>

            {/* KPI: Customers */}
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition-colors">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Clients enregistrés
                </span>
                <div className="p-2 bg-sky-50 text-sky-600 rounded-lg">
                  <Users className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-3">
                <div className="text-2xl font-bold text-slate-900">{totalCustomers}</div>
                <div className="mt-1 flex items-center justify-between text-xs text-slate-500">
                  <span>Portefeuille client</span>
                  <Link to="/customers" className="text-sky-600 font-semibold hover:underline">
                    Consulter &rarr;
                  </Link>
                </div>
              </div>
            </div>

            {/* KPI: Sales Count */}
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition-colors">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Commandes totales
                </span>
                <div className="p-2 bg-purple-50 text-purple-600 rounded-lg">
                  <ShoppingCart className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-3">
                <div className="text-2xl font-bold text-slate-900">{totalSales}</div>
                <div className="mt-1 flex items-center justify-between text-xs text-slate-500">
                  <span>Orchestrées par Saga</span>
                  <Link to="/sales" className="text-purple-600 font-semibold hover:underline">
                    Détails &rarr;
                  </Link>
                </div>
              </div>
            </div>

            {/* KPI: Recent Volume */}
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition-colors">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Volume échantillon récent
                </span>
                <div className="p-2 bg-amber-50 text-amber-600 rounded-lg">
                  <TrendingUp className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-3">
                <div className="text-2xl font-bold text-slate-900">
                  {formatCurrency(recentSalesAmount)}
                </div>
                <div className="mt-1 text-[11px] text-slate-400">
                  Somme des 5 dernières commandes
                </div>
              </div>
            </div>
          </div>

          {/* Activity Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Recent Orders Table (2 cols) */}
            <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
              <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Activity className="w-4 h-4 text-emerald-600" />
                  <h3 className="text-sm font-bold text-slate-900">Commandes Récentes</h3>
                </div>
                <Link
                  to="/sales"
                  className="text-xs font-semibold text-emerald-600 hover:text-emerald-700 flex items-center gap-1"
                >
                  <span>Voir l'historique complet</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>

              {recentSalesList.length === 0 ? (
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
                        <th className="px-4 py-3">Montant</th>
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
                              {sale.reference}
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
                              {sale.items?.length || 0} réf.
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

            {/* Status breakdown card (1 col) */}
            <div className="bg-white rounded-xl border border-slate-200 shadow-xs p-5 flex flex-col justify-between">
              <div>
                <h3 className="text-sm font-bold text-slate-900 mb-1">
                  État des Commandes Récentes
                </h3>
                <p className="text-xs text-slate-400 mb-4">
                  Répartition des 5 dernières commandes créées
                </p>

                <div className="space-y-3">
                  <div className="flex items-center justify-between p-2.5 rounded-lg bg-amber-50/70 border border-amber-100">
                    <div className="flex items-center gap-2 text-xs font-semibold text-amber-900">
                      <Clock className="w-4 h-4 text-amber-600" />
                      <span>En attente (PENDING)</span>
                    </div>
                    <span className="font-bold text-sm text-amber-900">{pendingCount}</span>
                  </div>

                  <div className="flex items-center justify-between p-2.5 rounded-lg bg-sky-50/70 border border-sky-100">
                    <div className="flex items-center gap-2 text-xs font-semibold text-sky-900">
                      <CheckCircle className="w-4 h-4 text-sky-600" />
                      <span>Confirmées (CONFIRMED)</span>
                    </div>
                    <span className="font-bold text-sm text-sky-900">{confirmedCount}</span>
                  </div>

                  <div className="flex items-center justify-between p-2.5 rounded-lg bg-emerald-50/70 border border-emerald-100">
                    <div className="flex items-center gap-2 text-xs font-semibold text-emerald-900">
                      <CheckCircle className="w-4 h-4 text-emerald-600" />
                      <span>Terminées (COMPLETED)</span>
                    </div>
                    <span className="font-bold text-sm text-emerald-900">{completedCount}</span>
                  </div>

                  <div className="flex items-center justify-between p-2.5 rounded-lg bg-rose-50/70 border border-rose-100">
                    <div className="flex items-center gap-2 text-xs font-semibold text-rose-900">
                      <Ban className="w-4 h-4 text-rose-600" />
                      <span>Annulées (CANCELLED)</span>
                    </div>
                    <span className="font-bold text-sm text-rose-900">{cancelledCount}</span>
                  </div>
                </div>
              </div>

              <div className="mt-5 pt-4 border-t border-slate-100">
                <Link to="/sales">
                  <Button variant="outline" size="sm" className="w-full">
                    Gérer les statuts de commande
                  </Button>
                </Link>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};
