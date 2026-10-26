import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { CustomerRequest, CustomerResponse } from '@/types/customer';
import { Modal } from '@/components/common/Modal';
import { Input } from '@/components/common/Input';
import { Button } from '@/components/common/Button';

const customerSchema = z.object({
  firstName: z
    .string()
    .min(1, 'Le prénom est obligatoire.')
    .max(50, 'Le prénom ne doit pas dépasser 50 caractères.'),
  lastName: z
    .string()
    .min(1, 'Le nom de famille est obligatoire.')
    .max(50, 'Le nom ne doit pas dépasser 50 caractères.'),
  email: z
    .string()
    .min(1, 'L’adresse email est obligatoire.')
    .email('Format d’adresse email invalide.')
    .max(100, 'L’adresse email ne doit pas dépasser 100 caractères.'),
  phone: z.string().max(20, 'Le numéro de téléphone ne doit pas dépasser 20 caractères.').optional(),
  company: z.string().max(100, 'Le nom de l’entreprise ne doit pas dépasser 100 caractères.').optional(),
});

type CustomerFormData = z.infer<typeof customerSchema>;

export interface CustomerFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: CustomerRequest) => Promise<void>;
  customerToEdit?: CustomerResponse | null;
  isLoading?: boolean;
}

export const CustomerFormModal: React.FC<CustomerFormModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  customerToEdit,
  isLoading = false,
}) => {
  const isEditing = !!customerToEdit;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CustomerFormData>({
    resolver: zodResolver(customerSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      company: '',
    },
  });

  useEffect(() => {
    if (customerToEdit) {
      reset({
        firstName: customerToEdit.firstName,
        lastName: customerToEdit.lastName,
        email: customerToEdit.email,
        phone: customerToEdit.phone || '',
        company: customerToEdit.company || '',
      });
    } else {
      reset({
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        company: '',
      });
    }
  }, [customerToEdit, reset, isOpen]);

  const handleFormSubmit = async (data: CustomerFormData) => {
    await onSubmit({
      firstName: data.firstName.trim(),
      lastName: data.lastName.trim(),
      email: data.email.trim(),
      phone: data.phone?.trim() || undefined,
      company: data.company?.trim() || undefined,
    });
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditing ? 'Modifier la fiche client' : 'Créer un nouveau client'}
      description={
        isEditing
          ? `Mise à jour des coordonnées pour le client #${customerToEdit?.id}`
          : 'Enregistrez un nouveau profil client dans la base de données'
      }
      maxWidth="md"
    >
      <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Prénom"
            placeholder="ex: Jean"
            error={errors.firstName?.message}
            required
            {...register('firstName')}
          />
          <Input
            label="Nom"
            placeholder="ex: Dupont"
            error={errors.lastName?.message}
            required
            {...register('lastName')}
          />
        </div>

        <Input
          label="Adresse email"
          type="email"
          placeholder="ex: jean.dupont@entreprise.com"
          error={errors.email?.message}
          required
          {...register('email')}
        />

        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Téléphone professionnel"
            type="tel"
            placeholder="ex: +33612345678"
            error={errors.phone?.message}
            {...register('phone')}
          />
          <Input
            label="Entreprise / Organisation"
            placeholder="ex: Acme Corp"
            error={errors.company?.message}
            {...register('company')}
          />
        </div>

        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
          <Button variant="outline" type="button" onClick={onClose} disabled={isLoading}>
            Annuler
          </Button>
          <Button variant="primary" type="submit" isLoading={isLoading}>
            {isEditing ? 'Enregistrer les modifications' : 'Créer le client'}
          </Button>
        </div>
      </form>
    </Modal>
  );
};
