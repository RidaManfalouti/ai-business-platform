import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ProductRequest, ProductResponse } from '@/types/product';
import { Modal } from '@/components/common/Modal';
import { Input } from '@/components/common/Input';
import { Button } from '@/components/common/Button';

const productSchema = z.object({
  name: z
    .string()
    .min(1, 'Le nom du produit est obligatoire.')
    .max(100, 'Le nom ne doit pas dépasser 100 caractères.'),
  description: z.string().max(1000, 'La description ne doit pas dépasser 1000 caractères.').optional(),
  price: z.coerce
    .number({ invalid_type_error: 'Le prix doit être un nombre valide.' })
    .min(0, 'Le prix doit être supérieur ou égal à zéro.'),
  stockQuantity: z.coerce
    .number({ invalid_type_error: 'La quantité doit être un entier valide.' })
    .int('La quantité en stock doit être un entier.')
    .min(0, 'La quantité en stock ne peut pas être négative.'),
  category: z
    .string()
    .min(1, 'La catégorie est obligatoire.')
    .max(50, 'La catégorie ne doit pas dépasser 50 caractères.'),
});

type ProductFormData = z.infer<typeof productSchema>;

export interface ProductFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: ProductRequest) => Promise<void>;
  productToEdit?: ProductResponse | null;
  isLoading?: boolean;
}

export const ProductFormModal: React.FC<ProductFormModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  productToEdit,
  isLoading = false,
}) => {
  const isEditing = !!productToEdit;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ProductFormData>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      name: '',
      description: '',
      price: 0,
      stockQuantity: 0,
      category: '',
    },
  });

  useEffect(() => {
    if (productToEdit) {
      reset({
        name: productToEdit.name,
        description: productToEdit.description || '',
        price: productToEdit.price,
        stockQuantity: productToEdit.stockQuantity,
        category: productToEdit.category,
      });
    } else {
      reset({
        name: '',
        description: '',
        price: 0,
        stockQuantity: 0,
        category: '',
      });
    }
  }, [productToEdit, reset, isOpen]);

  const handleFormSubmit = async (data: ProductFormData) => {
    await onSubmit({
      name: data.name.trim(),
      description: data.description?.trim() || undefined,
      price: data.price,
      stockQuantity: data.stockQuantity,
      category: data.category.trim(),
    });
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditing ? 'Modifier le produit' : 'Ajouter un nouveau produit'}
      description={
        isEditing
          ? `Mise à jour des informations pour le produit #${productToEdit?.id}`
          : 'Enregistrez un nouveau produit dans le catalogue commercial'
      }
      maxWidth="md"
    >
      <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4">
        <Input
          label="Nom du produit"
          placeholder="ex: Clavier Mécanique RGB"
          error={errors.name?.message}
          required
          {...register('name')}
        />

        <Input
          label="Catégorie"
          placeholder="ex: Périphériques, Informatique, Audio..."
          error={errors.category?.message}
          required
          {...register('category')}
        />

        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Prix unitaire (€)"
            type="number"
            step="0.01"
            min="0"
            placeholder="0.00"
            error={errors.price?.message}
            required
            {...register('price')}
          />

          <Input
            label="Quantité en stock"
            type="number"
            step="1"
            min="0"
            placeholder="0"
            error={errors.stockQuantity?.message}
            required
            {...register('stockQuantity')}
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 mb-1.5">
            Description détaillée
          </label>
          <textarea
            rows={3}
            placeholder="Caractéristiques techniques, garantie, dimensions..."
            className="block w-full rounded-lg border border-slate-300 text-sm p-3 focus:outline-none focus:ring-2 focus:ring-emerald-100 focus:border-emerald-500 placeholder:text-slate-400"
            {...register('description')}
          />
          {errors.description && (
            <p className="mt-1 text-xs text-rose-600 font-medium">
              {errors.description.message}
            </p>
          )}
        </div>

        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
          <Button variant="outline" type="button" onClick={onClose} disabled={isLoading}>
            Annuler
          </Button>
          <Button variant="primary" type="submit" isLoading={isLoading}>
            {isEditing ? 'Mettre à jour' : 'Créer le produit'}
          </Button>
        </div>
      </form>
    </Modal>
  );
};
