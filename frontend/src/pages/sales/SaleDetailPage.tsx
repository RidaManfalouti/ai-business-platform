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
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-[#27272A]">
        <div className="flex items-center gap-3">
          <Link to="/sales">
            <Button variant="outline" size="sm" icon={<ArrowLeft className="w-4 h-4" />}>
              Retour
            </Button>
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl sm:text-2xl font-bold font-mono text-[#FAFAFA] tracking-tight">
                {sale.reference}
              </h1>
              <Badge variant={statusBadge.variant}>{statusBadge.label}</Badge>
            </div>
            <p className="text-xs text-[#A1A1AA] mt-0.5">Commande #{sale.id} • Enregistrée le {formatDate(sale.createdAt)}</p>
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
        <div className="bg-[#18181B] p-5 rounded-xl border border-[#27272A] shadow-lg">
          <div className="flex items-center gap-2 text-xs font-bold text-[#A1A1AA] uppercase tracking-wider mb-3">
            <User className="w-4 h-4 text-[#FACC15]" />
            <span>Client associé</span>
          </div>
          {customer ? (
            <div className="space-y-1">
              <div className="font-bold text-[#FAFAFA] text-sm">
                {customer.firstName} {customer.lastName}
              </div>
              <div className="text-xs text-[#A1A1AA]">{customer.email}</div>
              {customer.company && (
                <div className="text-xs text-[#71717A] font-medium">{customer.company}</div>
              )}
              {customer.phone && <div className="text-xs text-[#71717A]">{customer.phone}</div>}
            </div>
          ) : (
            <div className="text-xs text-[#A1A1AA]">
              Identifiant client : <strong className="text-[#FAFAFA]">#{sale.customerId}</strong>
            </div>
          )}
        </div>

        {/* Total & Amounts */}
        <div className="bg-[#18181B] p-5 rounded-xl border border-[#27272A] shadow-lg">
          <div className="flex items-center gap-2 text-xs font-bold text-[#A1A1AA] uppercase tracking-wider mb-3">
            <ShoppingBag className="w-4 h-4 text-[#FACC15]" />
            <span>Montant total</span>
          </div>
          <div className="text-2xl font-bold font-mono text-[#FACC15]">{formatCurrency(sale.totalAmount)}</div>
          <p className="text-[11px] text-[#71717A] mt-1">
            Recalculé par sales-service d'après le catalogue de prix
          </p>
        </div>

        {/* Saga Lifecycle State */}
        <div className="bg-[#18181B] p-5 rounded-xl border border-[#27272A] shadow-lg">
          <div className="flex items-center gap-2 text-xs font-bold text-[#A1A1AA] uppercase tracking-wider mb-3">
            <Clock className="w-4 h-4 text-[#A855F7]" />
            <span>Cycle de vie & Saga</span>
          </div>
          <div className="text-xs space-y-1.5 text-[#A1A1AA]">
            <div>
              Statut commande : <strong className="text-[#FAFAFA]">{sale.status}</strong>
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
      <div className="bg-[#18181B] rounded-xl border border-[#27272A] shadow-lg overflow-hidden">
        <div className="px-6 py-4 border-b border-[#27272A] bg-[#202024]/60">
          <h3 className="text-sm font-bold text-[#FAFAFA]">
            Articles de la commande ({sale.items?.length || 0})
          </h3>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-[#A1A1AA] divide-y divide-[#27272A]">
            <thead className="bg-[#131316] text-[11px] font-semibold uppercase text-[#A1A1AA]">
              <tr>
                <th className="px-6 py-3">Ligne #</th>
                <th className="px-6 py-3">Réf. Produit</th>
                <th className="px-6 py-3">Désignation</th>
                <th className="px-6 py-3">Prix unitaire figé</th>
                <th className="px-6 py-3 text-center">Quantité</th>
                <th className="px-6 py-3 text-right">Sous-total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#27272A]">
              {sale.items?.map((item) => (
                <tr key={item.id} className="hover:bg-[#202024]/50 transition-colors">
                  <td className="px-6 py-3.5 font-mono text-[#71717A]">#{item.id}</td>
                  <td className="px-6 py-3.5 font-mono text-[#A1A1AA]">#{item.productId}</td>
                  <td className="px-6 py-3.5 font-semibold text-[#FAFAFA]">
                    {item.productNameSnapshot}
                  </td>
                  <td className="px-6 py-3.5">{formatCurrency(item.unitPrice)}</td>
                  <td className="px-6 py-3.5 text-center font-bold text-[#FAFAFA]">{item.quantity}</td>
                  <td className="px-6 py-3.5 text-right font-mono font-bold text-[#FAFAFA]">
                    {formatCurrency(item.lineTotal)}
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot className="bg-[#131316] font-bold text-[#FAFAFA] border-t border-[#27272A]">
              <tr>
                <td colSpan={5} className="px-6 py-3.5 text-right uppercase text-[11px] text-[#A1A1AA]">
                  Total TTC
                </td>
                <td className="px-6 py-3.5 text-right font-mono text-base text-[#FACC15]">
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
