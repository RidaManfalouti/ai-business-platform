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
          className="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 transition text-left focus:outline-none focus:ring-2 focus:ring-indigo-500/20 shadow-xs"
        >
          <div className="flex items-center justify-center w-7 h-7 rounded-md bg-indigo-50 text-indigo-600">
            <Building2 className="w-4 h-4" />
          </div>
          <div className="flex flex-col">
            <span className="text-xs font-semibold text-slate-800 truncate max-w-[140px] sm:max-w-[180px]">
              {displayName}
            </span>
            <span className="text-[10px] text-slate-400 font-medium">
              {activeTenantRole ? `Rôle : ${activeTenantRole}` : 'Locataire'}
            </span>
          </div>
          <ChevronDown className="w-3.5 h-3.5 text-slate-400 ml-1" />
        </button>

        {isOpen && (
          <div className="absolute left-0 sm:left-auto sm:right-0 mt-2 w-72 rounded-xl bg-white shadow-xl border border-slate-100 py-2 z-50">
            <div className="px-3 py-1.5 border-b border-slate-100 mb-1 flex items-center justify-between">
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                Mes Entreprises
              </span>
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
            </div>

            <div className="max-h-60 overflow-y-auto py-1">
              {availableTenants.length === 0 ? (
                <div className="px-3 py-2 text-xs text-slate-500 text-center">
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
                      className={`w-full flex items-center justify-between px-3 py-2 text-left hover:bg-slate-50 transition ${
                        isCurrent ? 'bg-indigo-50/50' : ''
                      }`}
                    >
                      <div className="flex items-center gap-2.5 truncate mr-2">
                        <Building2
                          className={`w-4 h-4 shrink-0 ${
                            isCurrent ? 'text-indigo-600' : 'text-slate-400'
                          }`}
                        />
                        <div className="truncate">
                          <div className="text-xs font-medium text-slate-800 truncate">
                            {tenant.tenantName}
                          </div>
                          <div className="text-[10px] text-slate-400 truncate">
                            {tenant.tenantSlug}
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-1.5 shrink-0">
                        <Badge variant="default" size="sm" className="text-[10px] py-0 px-1.5">
                          {tenant.role}
                        </Badge>
                        {isCurrent && <Check className="w-4 h-4 text-indigo-600" />}
                      </div>
                    </button>
                  );
                })
              )}
            </div>

            <div className="border-t border-slate-100 mt-1 pt-1 px-1">
              <button
                type="button"
                onClick={() => {
                  setIsOpen(false);
                  setIsModalOpen(true);
                }}
                className="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-medium text-indigo-600 hover:bg-indigo-50 transition"
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
            <div className="p-3 text-xs text-rose-700 bg-rose-50 border border-rose-200 rounded-lg">
              {error}
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">
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
            <label className="block text-xs font-medium text-slate-700 mb-1">
              Identifiant / Slug (optionnel)
            </label>
            <Input
              type="text"
              placeholder="Ex: acme-corp"
              value={newTenantSlug}
              onChange={(e) => setNewTenantSlug(e.target.value)}
            />
            <p className="text-[11px] text-slate-400 mt-1">
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
