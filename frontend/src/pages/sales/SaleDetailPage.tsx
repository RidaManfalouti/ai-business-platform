import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { salesApi } from '@/api/salesApi';
import { customersApi } from '@/api/customersApi';
import { SaleStatus } from '@/types/sale';
import { formatCurrency, formatDate, getSaleStatusBadge } from '@/utils/formatters';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { Alert } from '@/components/common/Alert';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ConfirmDialog } from '@/components/common/ConfirmDialog';
import {
  ArrowLeft,
  CheckCircle,
  XCircle,
  Clock,
  User,
  ShoppingBag,
} from 'lucide-react';
import { ParsedApiError } from '@/types/api';

export const SaleDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const saleId = Number(id);
  const queryClient = useQueryClient();

  const [confirmCancelOpen, setConfirmCancelOpen] = useState(false);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const {
    data: sale,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['sale', saleId],
    queryFn: () => salesApi.getSaleById(saleId),
    enabled: !isNaN(saleId),
  });

  // Query customer details for this sale
  const { data: customer } = useQuery({
    queryKey: ['customer', sale?.customerId],
    queryFn: () => customersApi.getCustomerById(sale!.customerId),
    enabled: !!sale?.customerId,
  });

  // Status transition mutation
  const updateStatusMutation = useMutation({
    mutationFn: (newStatus: SaleStatus) =>
      salesApi.updateSaleStatus(saleId, { status: newStatus }),
    onSuccess: (updated) => {
      queryClient.setQueryData(['sale', saleId], updated);
      queryClient.invalidateQueries({ queryKey: ['sales'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      setActionSuccess(`Statut mis à jour vers "${updated.status}".`);
    },
    onError: (err: ParsedApiError) => {
      setActionError(err.message || 'Impossible de mettre à jour le statut.');
    },
  });

  // Cancellation mutation (triggers compensation saga)
  const cancelMutation = useMutation({
    mutationFn: () => salesApi.cancelSale(saleId),
    onSuccess: (updated) => {
      queryClient.setQueryData(['sale', saleId], updated);
      queryClient.invalidateQueries({ queryKey: ['sales'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      setConfirmCancelOpen(false);
      setActionSuccess('Vente annulée avec succès et compensation de stock orchestrée.');
    },
    onError: (err: ParsedApiError) => {
      setConfirmCancelOpen(false);
      setActionError(
        err.message || 'Échec lors de l’annulation de la commande et de la compensation de stock.'
      );
    },
  });

  if (isLoading) {
    return <LoadingSpinner label="Chargement des détails de la commande..." />;
  }

  if (error || !sale) {
    return (
      <div className="space-y-4">
        <Link to="/sales">
          <Button variant="outline" size="sm" icon={<ArrowLeft className="w-4 h-4" />}>
            Retour aux ventes
          </Button>
        </Link>
        <Alert variant="error" title="Commande introuvable">
          {(error as unknown as ParsedApiError)?.message || 'La commande spécifiée n’existe pas ou a été supprimée.'}
        </Alert>
      </div>
    );
  }

  const statusBadge = getSaleStatusBadge(sale.status);
  const isPending = sale.status === 'PENDING';
  const isConfirmed = sale.status === 'CONFIRMED';
  const isTerminal = sale.status === 'COMPLETED' || sale.status === 'CANCELLED';

  return (
    <div className="space-y-6">
      {/* Top back navigation & actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-slate-200">
        <div className="flex items-center gap-3">
          <Link to="/sales">
            <Button variant="outline" size="sm" icon={<ArrowLeft className="w-4 h-4" />}>
              Retour
            </Button>
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl sm:text-2xl font-bold font-mono text-slate-900 tracking-tight">
                {sale.reference}
              </h1>
              <Badge variant={statusBadge.variant}>{statusBadge.label}</Badge>
            </div>
            <p className="text-xs text-slate-400 mt-0.5">Commande #{sale.id} • Enregistrée le {formatDate(sale.createdAt)}</p>
          </div>
        </div>

        {/* State Machine Transition Buttons */}
        <div className="flex items-center gap-2">
          {isPending && (
            <Button
              variant="primary"
              size="sm"
              icon={<CheckCircle className="w-4 h-4" />}
              isLoading={updateStatusMutation.isPending}
              onClick={() => updateStatusMutation.mutate('CONFIRMED')}
            >
              Confirmer la commande
            </Button>
          )}

          {isConfirmed && (
            <Button
              variant="primary"
              size="sm"
              icon={<CheckCircle className="w-4 h-4" />}
              isLoading={updateStatusMutation.isPending}
              onClick={() => updateStatusMutation.mutate('COMPLETED')}
            >
              Terminer la commande
            </Button>
          )}

          {!isTerminal && (
            <Button
              variant="danger"
              size="sm"
              icon={<XCircle className="w-4 h-4" />}
              isLoading={cancelMutation.isPending}
              onClick={() => setConfirmCancelOpen(true)}
            >
              Annuler la vente
            </Button>
          )}
        </div>
      </div>

      {/* Notifications */}
      {actionSuccess && (
        <Alert variant="success" onDismiss={() => setActionSuccess(null)}>
          {actionSuccess}
        </Alert>
      )}

      {actionError && (
        <Alert variant="error" onDismiss={() => setActionError(null)}>
          {actionError}
        </Alert>
      )}

      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Customer Card */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-600 uppercase tracking-wider mb-3">
            <User className="w-4 h-4 text-emerald-600" />
            <span>Client associé</span>
          </div>
          {customer ? (
            <div className="space-y-1">
              <div className="font-bold text-slate-900 text-sm">
                {customer.firstName} {customer.lastName}
              </div>
              <div className="text-xs text-slate-500">{customer.email}</div>
              {customer.company && (
                <div className="text-xs text-slate-400 font-medium">{customer.company}</div>
              )}
              {customer.phone && <div className="text-xs text-slate-400">{customer.phone}</div>}
            </div>
          ) : (
            <div className="text-xs text-slate-500">
              Identifiant client : <strong>#{sale.customerId}</strong>
            </div>
          )}
        </div>

        {/* Total & Amounts */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-600 uppercase tracking-wider mb-3">
            <ShoppingBag className="w-4 h-4 text-emerald-600" />
            <span>Montant total</span>
          </div>
          <div className="text-2xl font-bold text-slate-900">{formatCurrency(sale.totalAmount)}</div>
          <p className="text-[11px] text-slate-400 mt-1">
            Recalculé par sales-service d'après le catalogue de prix
          </p>
        </div>

        {/* Saga Lifecycle State */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-600 uppercase tracking-wider mb-3">
            <Clock className="w-4 h-4 text-emerald-600" />
            <span>Cycle de vie & Saga</span>
          </div>
          <div className="text-xs space-y-1 text-slate-600">
            <div>
              Statut commande : <strong>{sale.status}</strong>
            </div>
            <div>
              Date de création : <span>{formatDate(sale.createdAt)}</span>
            </div>
            {sale.updatedAt && (
              <div>
                Dernière mise à jour : <span>{formatDate(sale.updatedAt)}</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Items Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 bg-slate-50/50">
          <h3 className="text-sm font-bold text-slate-900">
            Articles de la commande ({sale.items?.length || 0})
          </h3>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-600 divide-y divide-slate-100">
            <thead className="bg-slate-50 text-[11px] font-semibold uppercase text-slate-500">
              <tr>
                <th className="px-6 py-3">Ligne #</th>
                <th className="px-6 py-3">Réf. Produit</th>
                <th className="px-6 py-3">Désignation</th>
                <th className="px-6 py-3">Prix unitaire figé</th>
                <th className="px-6 py-3 text-center">Quantité</th>
                <th className="px-6 py-3 text-right">Sous-total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {sale.items?.map((item) => (
                <tr key={item.id} className="hover:bg-slate-50/60 transition-colors">
                  <td className="px-6 py-3.5 font-mono text-slate-400">#{item.id}</td>
                  <td className="px-6 py-3.5 font-mono text-slate-600">#{item.productId}</td>
                  <td className="px-6 py-3.5 font-semibold text-slate-900">
                    {item.productNameSnapshot}
                  </td>
                  <td className="px-6 py-3.5">{formatCurrency(item.unitPrice)}</td>
                  <td className="px-6 py-3.5 text-center font-bold">{item.quantity}</td>
                  <td className="px-6 py-3.5 text-right font-bold text-slate-900">
                    {formatCurrency(item.lineTotal)}
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot className="bg-slate-50/80 font-bold text-slate-900 border-t border-slate-200">
              <tr>
                <td colSpan={5} className="px-6 py-3.5 text-right uppercase text-[11px] text-slate-500">
                  Total TTC
                </td>
                <td className="px-6 py-3.5 text-right text-base text-emerald-700">
                  {formatCurrency(sale.totalAmount)}
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>

      {/* Cancellation Confirmation Dialog */}
      {confirmCancelOpen && (
        <ConfirmDialog
          isOpen={confirmCancelOpen}
          onClose={() => setConfirmCancelOpen(false)}
          onConfirm={() => cancelMutation.mutate()}
          title="Annuler la commande de vente"
          message={`Êtes-vous certain de vouloir annuler la commande ${sale.reference} ? Cette action passera la vente en statut CANCELLED et déclenchera une Saga durable de compensation pour libérer les quantités réservées auprès de product-service.`}
          confirmLabel="Confirmer l'annulation"
          variant="danger"
          isLoading={cancelMutation.isPending}
        />
      )}
    </div>
  );
};
