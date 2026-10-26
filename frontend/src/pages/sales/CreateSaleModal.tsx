import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { productsApi } from '@/api/productsApi';
import { customersApi } from '@/api/customersApi';
import { CreateSaleRequest, SaleItemRequest } from '@/types/sale';
import { ProductResponse } from '@/types/product';
import { CustomerResponse } from '@/types/customer';
import { formatCurrency } from '@/utils/formatters';
import { generateSaleReference } from '@/utils/idempotency';
import { Modal } from '@/components/common/Modal';
import { Button } from '@/components/common/Button';
import { Input } from '@/components/common/Input';
import { Select } from '@/components/common/Select';
import { Alert } from '@/components/common/Alert';
import { Plus, Trash2, ShoppingCart, AlertCircle, RefreshCw } from 'lucide-react';

export interface CreateSaleModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (request: CreateSaleRequest, idempotencyKey: string) => Promise<void>;
  isLoading?: boolean;
  serverError?: string | null;
}

interface FormItem {
  productId: number;
  quantity: number;
}

export const CreateSaleModal: React.FC<CreateSaleModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  isLoading = false,
  serverError,
}) => {
  const [customerId, setCustomerId] = useState<number | ''>('');
  const [items, setItems] = useState<FormItem[]>([{ productId: 0, quantity: 1 }]);
  const [idempotencyKey, setIdempotencyKey] = useState<string>('');
  const [formValidationWarning, setFormValidationWarning] = useState<string | null>(null);

  // Fetch customers list for selection
  const { data: customersPage, isLoading: loadingCustomers } = useQuery({
    queryKey: ['customers', 'picker'],
    queryFn: () => customersApi.getCustomers({ page: 0, size: 100, sort: 'lastName,asc' }),
    enabled: isOpen,
  });

  // Fetch products list for item line selection
  const { data: productsPage, isLoading: loadingProducts } = useQuery({
    queryKey: ['products', 'picker'],
    queryFn: () => productsApi.getProducts({ page: 0, size: 100, sort: 'name,asc' }),
    enabled: isOpen,
  });

  const availableProducts: ProductResponse[] = productsPage?.content ?? [];
  const availableCustomers: CustomerResponse[] = customersPage?.content ?? [];

  // Generate an idempotency key once when modal opens
  useEffect(() => {
    if (isOpen) {
      setIdempotencyKey(generateSaleReference());
      setFormValidationWarning(null);
      setItems([{ productId: 0, quantity: 1 }]);
      setCustomerId('');
    }
  }, [isOpen]);

  const handleAddItem = () => {
    setItems((prev) => [...prev, { productId: 0, quantity: 1 }]);
  };

  const handleRemoveItem = (index: number) => {
    if (items.length <= 1) return;
    setItems((prev) => prev.filter((_, idx) => idx !== index));
  };

  const handleItemProductChange = (index: number, newProductId: number) => {
    setItems((prev) =>
      prev.map((item, idx) => (idx === index ? { ...item, productId: newProductId } : item))
    );
  };

  const handleItemQuantityChange = (index: number, qty: number) => {
    setItems((prev) =>
      prev.map((item, idx) => (idx === index ? { ...item, quantity: Math.max(1, qty) } : item))
    );
  };

  // Compute calculated subtotal based on current selected products
  const computedTotal = items.reduce((sum, item) => {
    const prod = availableProducts.find((p) => p.id === item.productId);
    if (!prod) return sum;
    return sum + (prod.price || 0) * (item.quantity || 1);
  }, 0);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormValidationWarning(null);

    if (!customerId) {
      setFormValidationWarning('Veuillez sélectionner un client.');
      return;
    }

    if (items.length === 0 || items.some((i) => !i.productId || i.quantity < 1)) {
      setFormValidationWarning('Toutes les lignes doivent comporter un produit valide et une quantité positive.');
      return;
    }

    // Check if duplicate products in list
    const productIds = items.map((i) => i.productId);
    if (new Set(productIds).size !== productIds.length) {
      setFormValidationWarning('Chaque produit ne doit apparaître qu’une seule fois dans la commande. Ajustez plutôt la quantité.');
      return;
    }

    // Check client-side stock warning
    for (const item of items) {
      const prod = availableProducts.find((p) => p.id === item.productId);
      if (prod && prod.stockQuantity < item.quantity) {
        setFormValidationWarning(
          `Stock insuffisant pour "${prod.name}" (demandé : ${item.quantity}, disponible : ${prod.stockQuantity}).`
        );
        return;
      }
    }

    const saleItems: SaleItemRequest[] = items.map((i) => ({
      productId: Number(i.productId),
      quantity: Number(i.quantity),
    }));

    await onSubmit(
      {
        customerId: Number(customerId),
        reference: idempotencyKey,
        items: saleItems,
      },
      idempotencyKey
    );
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Créer une commande de vente"
      description="Réservation atomique de stock orchestrée par Saga et calcul des montants côté serveur"
      maxWidth="xl"
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        {serverError && (
          <Alert variant="error" title="Échec de la réservation / création">
            <div className="flex items-start gap-2 mt-0.5">
              <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
              <span>{serverError}</span>
            </div>
          </Alert>
        )}

        {formValidationWarning && (
          <Alert variant="warning" onDismiss={() => setFormValidationWarning(null)}>
            {formValidationWarning}
          </Alert>
        )}

        {/* Idempotency Key Display */}
        <div className="p-3 bg-slate-50 rounded-lg border border-slate-200 flex items-center justify-between text-xs">
          <div>
            <span className="font-semibold text-slate-700">Clé d'idempotence / Réf. Vente : </span>
            <span className="font-mono text-emerald-700 font-bold ml-1">{idempotencyKey}</span>
          </div>
          <button
            type="button"
            onClick={() => setIdempotencyKey(generateSaleReference())}
            className="text-slate-500 hover:text-slate-800 flex items-center gap-1 font-medium"
            title="Générer une nouvelle référence"
          >
            <RefreshCw className="w-3 h-3" />
            <span>Régénérer</span>
          </button>
        </div>

        {/* Customer Select */}
        <div>
          <Select
            label="Client acheteur"
            required
            value={customerId}
            onChange={(e) => setCustomerId(e.target.value ? Number(e.target.value) : '')}
            options={[
              { value: '', label: loadingCustomers ? 'Chargement des clients...' : 'Sélectionnez un client...' },
              ...availableCustomers.map((c) => ({
                value: c.id,
                label: `${c.firstName} ${c.lastName} (${c.email})${c.company ? ` — ${c.company}` : ''}`,
              })),
            ]}
          />
        </div>

        {/* Order Items */}
        <div className="space-y-3 pt-2">
          <div className="flex items-center justify-between">
            <label className="text-xs font-bold uppercase tracking-wider text-slate-600">
              Articles commandés ({items.length})
            </label>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleAddItem}
              icon={<Plus className="w-3.5 h-3.5" />}
            >
              Ajouter une ligne
            </Button>
          </div>

          <div className="space-y-2.5 max-h-64 overflow-y-auto pr-1">
            {items.map((item, idx) => {
              const selectedProduct = availableProducts.find((p) => p.id === item.productId);
              const maxStock = selectedProduct?.stockQuantity ?? 0;
              const linePrice = (selectedProduct?.price || 0) * (item.quantity || 1);

              return (
                <div
                  key={idx}
                  className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5 p-3 rounded-lg border border-slate-200 bg-slate-50/50"
                >
                  <div className="flex-1">
                    <select
                      className="block w-full rounded-md border border-slate-300 bg-white text-xs py-2 px-2.5 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                      value={item.productId}
                      onChange={(e) => handleItemProductChange(idx, Number(e.target.value))}
                    >
                      <option value={0}>
                        {loadingProducts ? 'Chargement des produits...' : 'Sélectionnez un article...'}
                      </option>
                      {availableProducts.map((p) => (
                        <option key={p.id} value={p.id} disabled={p.stockQuantity <= 0}>
                          {p.name} — {formatCurrency(p.price)} (Stock: {p.stockQuantity})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="w-24">
                    <Input
                      type="number"
                      min={1}
                      max={maxStock > 0 ? maxStock : undefined}
                      value={item.quantity}
                      onChange={(e) => handleItemQuantityChange(idx, Number(e.target.value))}
                      placeholder="Qté"
                    />
                  </div>

                  <div className="w-28 text-right font-bold text-xs text-slate-800">
                    {formatCurrency(linePrice)}
                  </div>

                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    disabled={items.length <= 1}
                    onClick={() => handleRemoveItem(idx)}
                    className="text-slate-400 hover:text-rose-600 self-center"
                    aria-label="Supprimer cette ligne"
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
              );
            })}
          </div>
        </div>

        {/* Total & Summary footer */}
        <div className="pt-4 border-t border-slate-200 flex items-center justify-between">
          <div>
            <div className="text-xs text-slate-500">Estimation totale</div>
            <div className="text-xl font-bold text-slate-900">{formatCurrency(computedTotal)}</div>
            <div className="text-[10px] text-slate-400">Montant recalculé automatiquement par le serveur</div>
          </div>

          <div className="flex items-center gap-3">
            <Button variant="outline" type="button" onClick={onClose} disabled={isLoading}>
              Annuler
            </Button>
            <Button
              variant="primary"
              type="submit"
              isLoading={isLoading}
              icon={<ShoppingCart className="w-4 h-4" />}
            >
              Confirmer la commande
            </Button>
          </div>
        </div>
      </form>
    </Modal>
  );
};
