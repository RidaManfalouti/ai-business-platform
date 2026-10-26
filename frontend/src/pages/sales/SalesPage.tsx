import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { salesApi } from '@/api/salesApi';
import { CreateSaleRequest, SaleResponse } from '@/types/sale';
import { formatCurrency, formatDate, getSaleStatusBadge } from '@/utils/formatters';
import { Table, TableHead, TableBody, TableRow, TableHeaderCell, TableCell } from '@/components/common/Table';
import { Pagination } from '@/components/common/Pagination';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { Alert } from '@/components/common/Alert';
import { EmptyState } from '@/components/common/EmptyState';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ConfirmDialog } from '@/components/common/ConfirmDialog';
import { CreateSaleModal } from './CreateSaleModal';
import { Plus, ShoppingCart, Eye, XCircle } from 'lucide-react';
import { ParsedApiError } from '@/types/api';

export const SalesPage: React.FC = () => {
  const queryClient = useQueryClient();

  const [page, setPage] = useState<number>(0);
  const [pageSize] = useState<number>(10);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  const [isCreateOpen, setIsCreateOpen] = useState<boolean>(false);
  const [saleToCancel, setSaleToCancel] = useState<SaleResponse | null>(null);

  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [createError, setCreateError] = useState<string | null>(null);

  const {
    data: salesPage,
    isLoading,
    isFetching,
    error,
  } = useQuery({
    queryKey: ['sales', page, pageSize],
    queryFn: () =>
      salesApi.getSales({
        page,
        size: pageSize,
        sort: 'createdAt,desc',
      }),
  });

  // Create sale mutation
  const createMutation = useMutation({
    mutationFn: ({ request, idempotencyKey }: { request: CreateSaleRequest; idempotencyKey: string }) =>
      salesApi.createSale(request, idempotencyKey),
    onSuccess: (newSale) => {
      queryClient.invalidateQueries({ queryKey: ['sales'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      setIsCreateOpen(false);
      setCreateError(null);
      setActionSuccess(`Commande "${newSale.reference}" créée avec succès.`);
    },
    onError: (err: ParsedApiError) => {
      setCreateError(err.message || 'Impossible de créer la vente.');
    },
  });

  // Cancel sale mutation (triggers Saga compensation)
  const cancelMutation = useMutation({
    mutationFn: (saleId: number) => salesApi.cancelSale(saleId),
    onSuccess: (cancelledSale) => {
      queryClient.invalidateQueries({ queryKey: ['sales'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      setSaleToCancel(null);
      setActionSuccess(`Commande "${cancelledSale.reference}" annulée avec succès (stock libéré).`);
    },
    onError: (err: ParsedApiError) => {
      setActionError(err.message || 'Impossible d’annuler la commande.');
    },
  });

  const handleCreateSubmit = async (request: CreateSaleRequest, idempotencyKey: string) => {
    setCreateError(null);
    await createMutation.mutateAsync({ request, idempotencyKey });
  };

  const handleCancelConfirm = async () => {
    if (saleToCancel) {
      await cancelMutation.mutateAsync(saleToCancel.id);
    }
  };

  const allSales = salesPage?.content ?? [];
  // Client-side filter for quick status filtering
  const filteredSales = statusFilter === 'ALL'
    ? allSales
    : allSales.filter((s) => s.status === statusFilter);

  const totalElements = salesPage?.totalElements ?? 0;
  const totalPages = salesPage?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      {/* Header bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
            Ventes & Commandes
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Gestion des commandes clients et compensation de stock par Saga durable (microservice <code>sales-service</code>)
          </p>
        </div>

        <Button
          variant="primary"
          size="sm"
          onClick={() => {
            setCreateError(null);
            setIsCreateOpen(true);
          }}
          icon={<Plus className="w-4 h-4" />}
        >
          Nouvelle Vente
        </Button>
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

      {error && (
        <Alert variant="error" title="Erreur lors de la récupération des ventes">
          {(error as unknown as ParsedApiError).message || 'Impossible de joindre le service des ventes.'}
        </Alert>
      )}

      {/* Filter Tabs Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs">
        <div className="flex items-center gap-1.5 overflow-x-auto w-full sm:w-auto">
          {['ALL', 'PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'].map((status) => (
            <button
              key={status}
              type="button"
              onClick={() => setStatusFilter(status)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                statusFilter === status
                  ? 'bg-slate-900 text-white'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {status === 'ALL' ? 'Toutes' : status}
            </button>
          ))}
        </div>

        <div className="text-xs text-slate-500 flex items-center gap-2">
          {isFetching && <span className="text-emerald-600 font-medium">Actualisation...</span>}
          <span>
            Total : <strong className="text-slate-800">{totalElements}</strong> commande(s)
          </span>
        </div>
      </div>

      {/* Main Table or Loading/Empty States */}
      {isLoading ? (
        <LoadingSpinner label="Chargement des commandes..." />
      ) : filteredSales.length === 0 ? (
        <EmptyState
          title="Aucune commande trouvée"
          description={
            statusFilter !== 'ALL'
              ? `Aucune commande avec le statut "${statusFilter}".`
              : 'Aucune commande client n’a encore été passée.'
          }
          icon={<ShoppingCart className="w-8 h-8 text-slate-400" />}
          actionLabel="Créer une première commande"
          onAction={() => setIsCreateOpen(true)}
        />
      ) : (
        <div className="space-y-0">
          <Table>
            <TableHead>
              <tr>
                <TableHeaderCell>Réf. Commande</TableHeaderCell>
                <TableHeaderCell>Client</TableHeaderCell>
                <TableHeaderCell>Statut</TableHeaderCell>
                <TableHeaderCell>Montant Total</TableHeaderCell>
                <TableHeaderCell>Articles</TableHeaderCell>
                <TableHeaderCell>Date</TableHeaderCell>
                <TableHeaderCell className="text-right">Actions</TableHeaderCell>
              </tr>
            </TableHead>
            <TableBody>
              {filteredSales.map((sale) => {
                const badge = getSaleStatusBadge(sale.status);
                const canCancel = sale.status === 'PENDING' || sale.status === 'CONFIRMED';

                return (
                  <TableRow key={sale.id}>
                    <TableCell className="font-mono font-bold text-slate-900">
                      <Link to={`/sales/${sale.id}`} className="hover:text-emerald-600 hover:underline">
                        {sale.reference}
                      </Link>
                    </TableCell>
                    <TableCell className="font-mono text-xs text-slate-600">
                      Client #{sale.customerId}
                    </TableCell>
                    <TableCell>
                      <Badge variant={badge.variant} size="sm">
                        {badge.label}
                      </Badge>
                    </TableCell>
                    <TableCell className="font-bold text-slate-900">
                      {formatCurrency(sale.totalAmount)}
                    </TableCell>
                    <TableCell className="text-slate-500 text-xs">
                      {sale.items?.length || 0} ligne(s)
                    </TableCell>
                    <TableCell className="text-xs text-slate-400">
                      {formatDate(sale.createdAt)}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <Link to={`/sales/${sale.id}`}>
                          <Button variant="ghost" size="sm" title="Consulter le détail" aria-label={`Consulter ${sale.reference}`}>
                            <Eye className="w-4 h-4 text-slate-600" />
                          </Button>
                        </Link>
                        {canCancel && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setSaleToCancel(sale)}
                            title="Annuler cette vente"
                            aria-label={`Annuler ${sale.reference}`}
                            className="hover:text-rose-600"
                          >
                            <XCircle className="w-4 h-4 text-rose-500" />
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>

          <Pagination
            currentPage={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={pageSize}
            onPageChange={(newPage) => setPage(newPage)}
            isLoading={isFetching}
          />
        </div>
      )}

      {/* Create Sale Modal */}
      {isCreateOpen && (
        <CreateSaleModal
          isOpen={isCreateOpen}
          onClose={() => setIsCreateOpen(false)}
          onSubmit={handleCreateSubmit}
          isLoading={createMutation.isPending}
          serverError={createError}
        />
      )}

      {/* Confirm Cancellation Dialog */}
      {saleToCancel && (
        <ConfirmDialog
          isOpen={!!saleToCancel}
          onClose={() => setSaleToCancel(null)}
          onConfirm={handleCancelConfirm}
          title="Annuler la commande"
          message={`Êtes-vous certain de vouloir annuler la commande ${saleToCancel.reference} ? Cette action passera la commande en CANCELLED et compensera le stock réservé auprès du catalogue.`}
          confirmLabel="Confirmer l'annulation"
          variant="danger"
          isLoading={cancelMutation.isPending}
        />
      )}
    </div>
  );
};
