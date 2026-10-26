import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { customersApi } from '@/api/customersApi';
import { salesApi } from '@/api/salesApi';
import { CustomerRequest, CustomerResponse } from '@/types/customer';
import { SaleResponse } from '@/types/sale';
import { useAuth } from '@/context/AuthContext';
import { formatDate, formatCurrency, getSaleStatusBadge } from '@/utils/formatters';
import { Table, TableHead, TableBody, TableRow, TableHeaderCell, TableCell } from '@/components/common/Table';
import { Pagination } from '@/components/common/Pagination';
import { Button } from '@/components/common/Button';
import { Input } from '@/components/common/Input';
import { Badge } from '@/components/common/Badge';
import { Alert } from '@/components/common/Alert';
import { EmptyState } from '@/components/common/EmptyState';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ConfirmDialog } from '@/components/common/ConfirmDialog';
import { Modal } from '@/components/common/Modal';
import { CustomerFormModal } from './CustomerFormModal';
import { Plus, Search, Edit2, Trash2, Users, ShoppingBag, Mail, Phone, Building } from 'lucide-react';
import { ParsedApiError } from '@/types/api';

export const CustomersPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { isAdmin } = useAuth();

  const [page, setPage] = useState<number>(0);
  const [pageSize] = useState<number>(10);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [debouncedSearch, setDebouncedSearch] = useState<string>('');

  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [selectedCustomer, setSelectedCustomer] = useState<CustomerResponse | null>(null);

  const [customerToDelete, setCustomerToDelete] = useState<CustomerResponse | null>(null);
  const [historyCustomer, setHistoryCustomer] = useState<CustomerResponse | null>(null);

  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Debounce search query
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchQuery);
      setPage(0);
    }, 350);
    return () => clearTimeout(handler);
  }, [searchQuery]);

  // Determine if search query is email-like or name-like
  const isEmailSearch = debouncedSearch.includes('@');

  const {
    data: customersPage,
    isLoading,
    isFetching,
    error,
  } = useQuery({
    queryKey: ['customers', page, pageSize, debouncedSearch],
    queryFn: () =>
      customersApi.getCustomers({
        page,
        size: pageSize,
        name: isEmailSearch ? undefined : debouncedSearch || undefined,
        email: isEmailSearch ? debouncedSearch || undefined : undefined,
      }),
  });

  // Query customer sales history when modal is opened
  const { data: customerSales, isLoading: loadingSales } = useQuery({
    queryKey: ['customerSales', historyCustomer?.id],
    queryFn: () => salesApi.getSalesByCustomer(historyCustomer!.id, { page: 0, size: 20 }),
    enabled: !!historyCustomer,
  });

  // Create customer mutation
  const createMutation = useMutation({
    mutationFn: (data: CustomerRequest) => customersApi.createCustomer(data),
    onSuccess: (newCust) => {
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      setIsModalOpen(false);
      setActionSuccess(`Client "${newCust.firstName} ${newCust.lastName}" créé avec succès.`);
    },
    onError: (err: ParsedApiError) => {
      setActionError(err.message || 'Impossible de créer le client.');
    },
  });

  // Update customer mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CustomerRequest }) =>
      customersApi.updateCustomer(id, data),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      setIsModalOpen(false);
      setSelectedCustomer(null);
      setActionSuccess(`Client "${updated.firstName} ${updated.lastName}" mis à jour.`);
    },
    onError: (err: ParsedApiError) => {
      setActionError(err.message || 'Impossible de modifier le client.');
    },
  });

  // Delete customer mutation (Admin only)
  const deleteMutation = useMutation({
    mutationFn: (id: number) => customersApi.deleteCustomer(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      setCustomerToDelete(null);
      setActionSuccess('Fiche client supprimée.');
    },
    onError: (err: ParsedApiError) => {
      setActionError(err.message || 'Impossible de supprimer le client.');
    },
  });

  const handleOpenCreate = () => {
    setSelectedCustomer(null);
    setActionError(null);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (customer: CustomerResponse) => {
    setSelectedCustomer(customer);
    setActionError(null);
    setIsModalOpen(true);
  };

  const handleFormSubmit = async (data: CustomerRequest) => {
    setActionError(null);
    if (selectedCustomer) {
      await updateMutation.mutateAsync({ id: selectedCustomer.id, data });
    } else {
      await createMutation.mutateAsync(data);
    }
  };

  const handleDeleteConfirm = async () => {
    if (customerToDelete) {
      await deleteMutation.mutateAsync(customerToDelete.id);
    }
  };

  const customers = customersPage?.content ?? [];
  const totalElements = customersPage?.totalElements ?? 0;
  const totalPages = customersPage?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      {/* Header bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
            Gestion des Clients
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Fichiers clients, coordonnées et historique de commandes (microservice <code>customer-service</code>)
          </p>
        </div>

        <Button variant="primary" size="sm" onClick={handleOpenCreate} icon={<Plus className="w-4 h-4" />}>
          Nouveau Client
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
        <Alert variant="error" title="Erreur lors de la récupération des clients">
          {(error as unknown as ParsedApiError).message || 'Impossible de joindre le service client.'}
        </Alert>
      )}

      {/* Filter / Search Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs">
        <div className="w-full sm:w-80">
          <Input
            placeholder="Rechercher par nom ou email..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            leftIcon={<Search className="w-4 h-4" />}
          />
        </div>

        <div className="text-xs text-slate-500 flex items-center gap-2">
          {isFetching && <span className="text-emerald-600 font-medium">Actualisation...</span>}
          <span>
            Total : <strong className="text-slate-800">{totalElements}</strong> client(s)
          </span>
        </div>
      </div>

      {/* Main Table or Loading/Empty States */}
      {isLoading ? (
        <LoadingSpinner label="Chargement des clients..." />
      ) : customers.length === 0 ? (
        <EmptyState
          title="Aucun client trouvé"
          description={
            debouncedSearch
              ? `Aucun résultat pour la recherche "${debouncedSearch}".`
              : 'Aucun profil client enregistré.'
          }
          icon={<Users className="w-8 h-8 text-slate-400" />}
          actionLabel="Créer un client"
          onAction={handleOpenCreate}
        />
      ) : (
        <div className="space-y-0">
          <Table>
            <TableHead>
              <tr>
                <TableHeaderCell>ID</TableHeaderCell>
                <TableHeaderCell>Client</TableHeaderCell>
                <TableHeaderCell>Email & Coordonnées</TableHeaderCell>
                <TableHeaderCell>Entreprise</TableHeaderCell>
                <TableHeaderCell>Inscrit le</TableHeaderCell>
                <TableHeaderCell className="text-right">Actions</TableHeaderCell>
              </tr>
            </TableHead>
            <TableBody>
              {customers.map((c) => (
                <TableRow key={c.id}>
                  <TableCell className="font-mono text-xs text-slate-400">#{c.id}</TableCell>
                  <TableCell>
                    <div className="font-semibold text-slate-900">
                      {c.firstName} {c.lastName}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1.5 text-xs text-slate-700">
                      <Mail className="w-3.5 h-3.5 text-slate-400" />
                      <span>{c.email}</span>
                    </div>
                    {c.phone && (
                      <div className="flex items-center gap-1.5 text-xs text-slate-400 mt-0.5">
                        <Phone className="w-3.5 h-3.5 text-slate-400" />
                        <span>{c.phone}</span>
                      </div>
                    )}
                  </TableCell>
                  <TableCell>
                    {c.company ? (
                      <div className="flex items-center gap-1.5 text-xs text-slate-700">
                        <Building className="w-3.5 h-3.5 text-slate-400" />
                        <span className="font-medium">{c.company}</span>
                      </div>
                    ) : (
                      <span className="text-xs text-slate-400 italic">Particulier</span>
                    )}
                  </TableCell>
                  <TableCell className="text-xs text-slate-400">
                    {formatDate(c.createdAt)}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setHistoryCustomer(c)}
                        title="Consulter l'historique d'achats"
                        aria-label={`Historique de ${c.firstName} ${c.lastName}`}
                      >
                        <ShoppingBag className="w-3.5 h-3.5 text-slate-600" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleOpenEdit(c)}
                        title="Modifier les coordonnées"
                        aria-label={`Modifier ${c.firstName} ${c.lastName}`}
                      >
                        <Edit2 className="w-3.5 h-3.5 text-slate-600" />
                      </Button>
                      {isAdmin && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setCustomerToDelete(c)}
                          title="Supprimer ce client"
                          aria-label={`Supprimer ${c.firstName} ${c.lastName}`}
                          className="hover:text-rose-600"
                        >
                          <Trash2 className="w-3.5 h-3.5 text-rose-500" />
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
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

      {/* Customer Create/Edit Modal */}
      {isModalOpen && (
        <CustomerFormModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSubmit={handleFormSubmit}
          customerToEdit={selectedCustomer}
          isLoading={createMutation.isPending || updateMutation.isPending}
        />
      )}

      {/* Customer Purchases Modal */}
      {historyCustomer && (
        <Modal
          isOpen={!!historyCustomer}
          onClose={() => setHistoryCustomer(null)}
          title={`Commandes de ${historyCustomer.firstName} ${historyCustomer.lastName}`}
          description={`Historique des transactions pour le client #${historyCustomer.id} (${historyCustomer.email})`}
          maxWidth="lg"
        >
          {loadingSales ? (
            <LoadingSpinner label="Récupération de l'historique des ventes..." />
          ) : !customerSales?.content || customerSales.content.length === 0 ? (
            <div className="p-8 text-center text-xs text-slate-500">
              Aucune commande enregistrée pour ce client.
            </div>
          ) : (
            <div className="space-y-3">
              <div className="text-xs text-slate-500 font-medium">
                {customerSales.content.length} commande(s) trouvée(s) :
              </div>
              <div className="divide-y divide-slate-100 max-h-80 overflow-y-auto border border-slate-200 rounded-lg">
                {customerSales.content.map((sale: SaleResponse) => {
                  const badge = getSaleStatusBadge(sale.status);
                  return (
                    <div key={sale.id} className="p-3.5 flex items-center justify-between text-xs hover:bg-slate-50">
                      <div>
                        <div className="font-mono font-bold text-slate-900">{sale.reference}</div>
                        <div className="text-slate-400 text-[11px] mt-0.5">{formatDate(sale.createdAt)}</div>
                      </div>
                      <div className="flex items-center gap-3">
                        <Badge variant={badge.variant} size="sm">
                          {badge.label}
                        </Badge>
                        <div className="font-bold text-slate-900">{formatCurrency(sale.totalAmount)}</div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </Modal>
      )}

      {/* Confirm Delete Dialog */}
      {customerToDelete && (
        <ConfirmDialog
          isOpen={!!customerToDelete}
          onClose={() => setCustomerToDelete(null)}
          onConfirm={handleDeleteConfirm}
          title="Supprimer la fiche client"
          message={`Êtes-vous certain de vouloir supprimer le profil de ${customerToDelete.firstName} ${customerToDelete.lastName} (ID: #${customerToDelete.id}) ?`}
          confirmLabel="Supprimer"
          variant="danger"
          isLoading={deleteMutation.isPending}
        />
      )}
    </div>
  );
};
