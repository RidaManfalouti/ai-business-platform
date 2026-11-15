import React, { useState, useRef, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Building2, ChevronDown, Check, Plus, ShieldCheck } from 'lucide-react';
import { Badge } from '@/components/common/Badge';
import { Modal } from '@/components/common/Modal';
import { Input } from '@/components/common/Input';
import { Button } from '@/components/common/Button';

export const TenantSwitcher: React.FC = () => {
  const {
    activeTenantId,
    activeTenantRole,
    availableTenants,
    switchTenant,
    createTenant,
  } = useAuth();

  const [isOpen, setIsOpen] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newTenantName, setNewTenantName] = useState('');
  const [newTenantSlug, setNewTenantSlug] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const activeTenant = availableTenants.find((t) => t.tenantId === activeTenantId);
  const displayName = activeTenant?.tenantName || activeTenant?.tenantSlug || 'Entreprise principale';

  const handleSelectTenant = async (tenantId: string) => {
    if (tenantId === activeTenantId) {
      setIsOpen(false);
      return;
    }
    setIsOpen(false);
    try {
      await switchTenant(tenantId);
    } catch (err: any) {
      console.error('Erreur lors du changement de locataire:', err);
    }
  };

  const handleCreateTenant = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTenantName.trim()) return;

    setIsSubmitting(true);
    setError(null);
    try {
      await createTenant(newTenantName.trim(), newTenantSlug.trim() || undefined);
      setIsModalOpen(false);
      setNewTenantName('');
      setNewTenantSlug('');
    } catch (err: any) {
      setError(err?.response?.data?.message || "Erreur lors de la création de l'entreprise");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <div className="relative" ref={dropdownRef}>
        <button
          type="button"
          onClick={() => setIsOpen(!isOpen)}
          className="flex items-center gap-2.5 px-3 py-1.5 rounded-xl border border-[#27272A] bg-[#18181B] hover:bg-[#202024] transition-all text-left focus:outline-none focus:ring-2 focus:ring-[#FACC15]/20 shadow-xs"
        >
          <div className="flex items-center justify-center w-7 h-7 rounded-lg bg-[#FACC15]/15 text-[#FACC15]">
            <Building2 className="w-4 h-4" />
          </div>
          <div className="flex flex-col">
            <span className="text-xs font-semibold text-[#FAFAFA] truncate max-w-[140px] sm:max-w-[180px]">
              {displayName}
            </span>
            <span className="text-[10px] text-[#A1A1AA] font-medium">
              {activeTenantRole ? `Rôle : ${activeTenantRole}` : 'Locataire'}
            </span>
          </div>
          <ChevronDown className="w-3.5 h-3.5 text-[#71717A] ml-1" />
        </button>

        {isOpen && (
          <div className="absolute left-0 sm:left-auto sm:right-0 mt-2 w-72 rounded-2xl bg-[#18181B] shadow-2xl border border-[#27272A] py-2 z-50 shadow-[0_20px_50px_rgba(0,0,0,0.8)]">
            <div className="px-3.5 py-2 border-b border-[#27272A] mb-1 flex items-center justify-between">
              <span className="text-[10px] font-bold text-[#71717A] uppercase tracking-wider">
                Mes Entreprises
              </span>
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
            </div>

            <div className="max-h-60 overflow-y-auto py-1">
              {availableTenants.length === 0 ? (
                <div className="px-3 py-2 text-xs text-[#71717A] text-center">
                  Aucune entreprise disponible
                </div>
              ) : (
                availableTenants.map((tenant) => {
                  const isCurrent = tenant.tenantId === activeTenantId;
                  return (
                    <button
                      key={tenant.tenantId}
                      type="button"
                      onClick={() => handleSelectTenant(tenant.tenantId)}
                      className={`w-full flex items-center justify-between px-3.5 py-2 text-left hover:bg-[#202024] transition-colors ${
                        isCurrent ? 'bg-[#FACC15]/10 border-l-2 border-l-[#FACC15]' : ''
                      }`}
                    >
                      <div className="flex items-center gap-2.5 truncate mr-2">
                        <Building2
                          className={`w-4 h-4 shrink-0 ${
                            isCurrent ? 'text-[#FACC15]' : 'text-[#71717A]'
                          }`}
                        />
                        <div className="truncate">
                          <div className={`text-xs font-medium truncate ${isCurrent ? 'text-[#FACC15] font-semibold' : 'text-[#FAFAFA]'}`}>
                            {tenant.tenantName}
                          </div>
                          <div className="text-[10px] text-[#71717A] truncate">
                            {tenant.tenantSlug}
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-1.5 shrink-0">
                        <Badge variant={isCurrent ? 'yellow' : 'default'} size="sm" className="text-[10px] py-0 px-1.5">
                          {tenant.role}
                        </Badge>
                        {isCurrent && <Check className="w-4 h-4 text-[#FACC15]" />}
                      </div>
                    </button>
                  );
                })
              )}
            </div>

            <div className="border-t border-[#27272A] mt-1 pt-1.5 px-2">
              <button
                type="button"
                onClick={() => {
                  setIsOpen(false);
                  setIsModalOpen(true);
                }}
                className="w-full flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-semibold text-[#FACC15] hover:bg-[#FACC15]/10 transition-colors"
              >
                <Plus className="w-4 h-4" />
                <span>Créer une nouvelle entreprise</span>
              </button>
            </div>
          </div>
        )}
      </div>

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Créer une entreprise (Locataire)"
        description="Créez un nouvel espace entreprise cloisonné. Vous recevrez automatiquement les droits OWNER."
      >
        <form onSubmit={handleCreateTenant} className="space-y-4">
          {error && (
            <div className="p-3 text-xs text-[#EF4444] bg-[#EF4444]/10 border border-[#EF4444]/20 rounded-xl">
              {error}
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-[#A1A1AA] uppercase tracking-wider mb-1.5">
              Nom de l'entreprise *
            </label>
            <Input
              type="text"
              required
              placeholder="Ex: Acme Corporation"
              value={newTenantName}
              onChange={(e) => setNewTenantName(e.target.value)}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-[#A1A1AA] uppercase tracking-wider mb-1.5">
              Identifiant / Slug (optionnel)
            </label>
            <Input
              type="text"
              placeholder="Ex: acme-corp"
              value={newTenantSlug}
              onChange={(e) => setNewTenantSlug(e.target.value)}
            />
            <p className="text-[11px] text-[#71717A] mt-1">
              S'il est omis, un slug unique sera généré automatiquement.
            </p>
          </div>

          <div className="flex justify-end gap-2 pt-3">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsModalOpen(false)}
            >
              Annuler
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={isSubmitting}
            >
              Créer et activer
            </Button>
          </div>
        </form>
      </Modal>
    </>
  );
};
