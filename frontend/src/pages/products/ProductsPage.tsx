import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productsApi } from '@/api/productsApi';
import { ProductRequest, ProductResponse } from '@/types/product';
import { useAuth } from '@/context/AuthContext';
import { formatCurrency, formatDate } from '@/utils/formatters';
import { Table, TableHead, TableBody, TableRow, TableHeaderCell, TableCell } from '@/components/common/Table';
import { Pagination } from '@/components/common/Pagination';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { Input } from '@/components/common/Input';
import { Alert } from '@/components/common/Alert';
import { EmptyState } from '@/components/common/EmptyState';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ConfirmDialog } from '@/components/common/ConfirmDialog';
import { ProductFormModal } from './ProductFormModal';
import { Plus, Search, Edit2, Trash2, Package, AlertCircle } from 'lucide-react';
import { ParsedApiError } from '@/types/api';

export const ProductsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { isAdmin } = useAuth();

  const [page, setPage] = useState<number>(0);
  const [pageSize] = useState<number>(10);
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [debouncedSearch, setDebouncedSearch] = useState<string>('');

  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [selectedProduct, setSelectedProduct] = useState<ProductResponse | null>(null);

  const [productToDelete, setProductToDelete] = useState<ProductResponse | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Debounce search input
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchTerm);
      setPage(0);
    }, 350);
    return () => clearTimeout(handler);
  }, [searchTerm]);

  const {
    data: productsPage,
    isLoading,
    isFetching,
    error,
  } = useQuery({
    queryKey: ['products', page, pageSize, debouncedSearch],
    queryFn: () =>
      productsApi.getProducts({
        page,
        size: pageSize,
        sort: 'id,asc',
        search: debouncedSearch,
      }),
  });

  // Create mutation
  const createMutation = useMutation({
    mutationFn: (data: ProductRequest) => productsApi.createProduct(data),
    onSuccess: (newProd) => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      setIsModalOpen(false);
      setActionSuccess(`Produit "${newProd.name}" créé avec succès.`);
    },
    onError: (err: ParsedApiError) => {
      setActionError(err.message || 'Impossible de créer le produit.');
    },
  });

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: ProductRequest }) =>
      productsApi.updateProduct(id, data),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      setIsModalOpen(false);
      setSelectedProduct(null);
      setActionSuccess(`Produit "${updated.name}" mis à jour avec succès.`);
    },
    onError: (err: ParsedApiError) => {
      setActionError(err.message || 'Impossible de modifier le produit.');
    },
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: (id: number) => productsApi.deleteProduct(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      setProductToDelete(null);
      setActionSuccess('Produit supprimé du catalogue.');
    },
    onError: (err: ParsedApiError) => {
      setActionError(err.message || 'Impossible de supprimer le produit.');
    },
  });

  const handleOpenCreate = () => {
    setSelectedProduct(null);
    setActionError(null);
    setIsModalOpen(true);
  };

  const handleOpenEdit = (product: ProductResponse) => {
    setSelectedProduct(product);
    setActionError(null);
    setIsModalOpen(true);
  };

  const handleFormSubmit = async (data: ProductRequest) => {
    setActionError(null);
    if (selectedProduct) {
      await updateMutation.mutateAsync({ id: selectedProduct.id, data });
    } else {
      await createMutation.mutateAsync(data);
    }
  };

  const handleDeleteConfirm = async () => {
    if (productToDelete) {
      await deleteMutation.mutateAsync(productToDelete.id);
    }
  };

  const products = productsPage?.content ?? [];
  const totalElements = productsPage?.totalElements ?? 0;
  const totalPages = productsPage?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      {/* Header bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-slate-200">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
            Catalogue de Produits
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Gestion des articles, stocks disponibles et inventaire (microservice <code>product-service</code>)
          </p>
        </div>

        {isAdmin && (
          <Button variant="primary" size="sm" onClick={handleOpenCreate} icon={<Plus className="w-4 h-4" />}>
            Nouveau Produit
          </Button>
        )}
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
        <Alert variant="error" title="Erreur lors de la récupération des produits">
          {(error as unknown as ParsedApiError).message || 'Impossible de joindre le service des produits.'}
        </Alert>
      )}

      {/* Filter / Search Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs">
        <div className="w-full sm:w-80">
          <Input
            placeholder="Rechercher par nom de produit..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            leftIcon={<Search className="w-4 h-4" />}
          />
        </div>

        <div className="text-xs text-slate-500 flex items-center gap-2">
          {isFetching && <span className="text-emerald-600 font-medium">Actualisation...</span>}
          <span>
            Total : <strong className="text-slate-800">{totalElements}</strong> produit(s)
          </span>
        </div>
      </div>

      {/* Main Table or Loading/Empty States */}
      {isLoading ? (
        <LoadingSpinner label="Chargement du catalogue..." />
      ) : products.length === 0 ? (
        <EmptyState
          title="Aucun produit trouvé"
          description={
            debouncedSearch
              ? `Aucun produit ne correspond au terme "${debouncedSearch}".`
              : 'Le catalogue est actuellement vide.'
          }
          icon={<Package className="w-8 h-8 text-slate-400" />}
          actionLabel={isAdmin ? 'Créer un premier produit' : undefined}
          onAction={isAdmin ? handleOpenCreate : undefined}
        />
      ) : (
        <div className="space-y-0">
          <Table>
            <TableHead>
              <tr>
                <TableHeaderCell>ID</TableHeaderCell>
                <TableHeaderCell>Produit & Catégorie</TableHeaderCell>
                <TableHeaderCell>Prix Unitaire</TableHeaderCell>
                <TableHeaderCell>Disponibilité Stock</TableHeaderCell>
                <TableHeaderCell>Créé le</TableHeaderCell>
                {isAdmin && <TableHeaderCell className="text-right">Actions</TableHeaderCell>}
              </tr>
            </TableHead>
            <TableBody>
              {products.map((p) => {
                const isOutOfStock = p.stockQuantity <= 0;
                const isLowStock = p.stockQuantity > 0 && p.stockQuantity <= 5;

                return (
                  <TableRow key={p.id}>
                    <TableCell className="font-mono text-xs text-slate-400">#{p.id}</TableCell>
                    <TableCell>
                      <div className="font-semibold text-slate-900">{p.name}</div>
                      <div className="text-xs text-slate-500 flex items-center gap-2 mt-0.5">
                        <span className="inline-block px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 text-[10px] font-medium">
                          {p.category}
                        </span>
                        {p.description && (
                          <span className="text-[11px] text-slate-400 truncate max-w-xs">
                            {p.description}
                          </span>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="font-bold text-slate-900">
                      {formatCurrency(p.price)}
                    </TableCell>
                    <TableCell>
                      {isOutOfStock ? (
                        <Badge variant="danger" size="sm" className="gap-1">
                          <AlertCircle className="w-3 h-3" />
                          Rupture (0)
                        </Badge>
                      ) : isLowStock ? (
                        <Badge variant="warning" size="sm">
                          Stock faible ({p.stockQuantity})
                        </Badge>
                      ) : (
                        <Badge variant="success" size="sm">
                          {p.stockQuantity} en stock
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-xs text-slate-400">
                      {formatDate(p.createdAt)}
                    </TableCell>
                    {isAdmin && (
                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleOpenEdit(p)}
                            title="Modifier ce produit"
                            aria-label={`Modifier ${p.name}`}
                          >
                            <Edit2 className="w-3.5 h-3.5 text-slate-600" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setProductToDelete(p)}
                            title="Supprimer définitivement"
                            aria-label={`Supprimer ${p.name}`}
                            className="hover:text-rose-600"
                          >
                            <Trash2 className="w-3.5 h-3.5 text-rose-500" />
                          </Button>
                        </div>
                      </TableCell>
                    )}
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

      {/* Product Create/Edit Modal */}
      {isModalOpen && (
        <ProductFormModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onSubmit={handleFormSubmit}
          productToEdit={selectedProduct}
          isLoading={createMutation.isPending || updateMutation.isPending}
        />
      )}

      {/* Confirm Delete Dialog */}
      {productToDelete && (
        <ConfirmDialog
          isOpen={!!productToDelete}
          onClose={() => setProductToDelete(null)}
          onConfirm={handleDeleteConfirm}
          title="Supprimer le produit"
          message={`Êtes-vous certain de vouloir supprimer le produit "${productToDelete.name}" (ID: #${productToDelete.id}) ? Cette action est irréversible et supprimera le produit du catalogue.`}
          confirmLabel="Supprimer"
          variant="danger"
          isLoading={deleteMutation.isPending}
        />
      )}
    </div>
  );
};
