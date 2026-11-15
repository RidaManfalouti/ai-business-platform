import React, { useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { Modal } from '@/components/common/Modal';
import { TenantSwitcher } from '@/components/layout/TenantSwitcher';
import { LogOut, Menu, User, Shield, Server, Clock, AlertTriangle } from 'lucide-react';

export interface HeaderProps {
  onToggleSidebar: () => void;
}

function formatRemainingTime(seconds: number | null): string {
  if (seconds === null) return '--';
  if (seconds <= 0) return 'Expiré';
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;
  if (hours > 0) return `${hours}h ${minutes}m`;
  if (minutes > 0) return `${minutes}m ${secs}s`;
  return `${secs}s`;
}

export const Header: React.FC<HeaderProps> = ({ onToggleSidebar }) => {
  const { user, isAdmin, logout, sessionRemainingSeconds, isSessionExpiringSoon, expireSessionNow } = useAuth();
  const [sessionModalOpen, setSessionModalOpen] = useState(false);

  return (
    <header className="sticky top-0 z-30 flex h-16 shrink-0 items-center justify-between border-b border-[#27272A] bg-[#09090B]/85 px-4 sm:px-6 backdrop-blur-md">
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onToggleSidebar}
          className="p-2 text-[#A1A1AA] rounded-xl hover:bg-[#202024] hover:text-[#FAFAFA] lg:hidden focus:outline-none transition-colors"
          aria-label="Ouvrir le menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        {/* Gateway connection pill */}
        <div className="hidden sm:flex items-center gap-2 px-3 py-1 rounded-full bg-[#18181B] text-[#A1A1AA] text-xs border border-[#27272A]">
          <Server className="w-3.5 h-3.5 text-[#FACC15]" />
          <span>Passerelle API</span>
          <span className="inline-block w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
        </div>

        {/* Session Expiry Status Pill */}
        {user && sessionRemainingSeconds !== null && (
          <button
            type="button"
            onClick={() => setSessionModalOpen(true)}
            className={`hidden md:flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs border transition-colors cursor-pointer ${
              isSessionExpiringSoon
                ? 'bg-amber-950/40 border-amber-500/50 text-amber-300 animate-pulse'
                : 'bg-[#18181B] border-[#27272A] text-[#A1A1AA] hover:text-[#FAFAFA] hover:border-[#3F3F46]'
            }`}
            title="Détails de la session JWT et expiration"
          >
            <Clock className={`w-3.5 h-3.5 ${isSessionExpiringSoon ? 'text-amber-400' : 'text-[#FACC15]'}`} />
            <span>Session :</span>
            <span className="font-mono font-bold text-[#FAFAFA]">
              {formatRemainingTime(sessionRemainingSeconds)}
            </span>
          </button>
        )}
      </div>

      <div className="flex items-center gap-3 sm:gap-4">
        {user && <TenantSwitcher />}

        {user && (
          <div className="flex items-center gap-2.5 text-right">
            <div className="hidden md:block">
              <div className="text-xs font-bold text-[#FAFAFA]">{user.username}</div>
              <div className="text-[11px] text-[#71717A] max-w-[160px] truncate">{user.email}</div>
            </div>

            <div className="flex items-center gap-1.5">
              <Badge variant={isAdmin ? 'purple' : 'default'} size="sm" className="gap-1">
                {isAdmin ? <Shield className="w-3 h-3 text-[#A855F7]" /> : <User className="w-3 h-3" />}
                {isAdmin ? 'ADMIN' : 'USER'}
              </Badge>
            </div>
          </div>
        )}

        <div className="h-6 w-px bg-[#27272A]" />

        <Button
          variant="ghost"
          size="sm"
          onClick={logout}
          icon={<LogOut className="w-4 h-4 text-[#71717A]" />}
          className="text-[#A1A1AA] hover:text-[#EF4444]"
          title="Se déconnecter"
        >
          <span className="hidden sm:inline">Déconnexion</span>
        </Button>
      </div>

      {/* Session Details & Simulation Modal */}
      {sessionModalOpen && (
        <Modal
          isOpen={sessionModalOpen}
          onClose={() => setSessionModalOpen(false)}
          title="Sécurité & Expiration de Session JWT"
          description="Contrôle d'intégrité de la session d'authentification cliente"
          maxWidth="md"
        >
          <div className="space-y-4">
            <div className="p-4 bg-[#202024] rounded-xl border border-[#27272A] space-y-2.5 text-xs">
              <div className="flex justify-between items-center pb-2 border-b border-[#27272A]">
                <span className="text-[#A1A1AA]">Statut :</span>
                <span className="font-semibold text-emerald-400 flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                  Session Active (Vérifiée)
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-[#A1A1AA]">Utilisateur :</span>
                <span className="font-semibold text-[#FAFAFA]">{user?.email}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-[#A1A1AA]">Rôle Plateforme :</span>
                <span className="font-semibold text-[#FACC15]">{isAdmin ? 'ROLE_ADMIN' : 'ROLE_USER'}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-[#A1A1AA]">Temps restant avant expiration :</span>
                <span className="font-mono font-bold text-[#FAFAFA] text-sm">
                  {formatRemainingTime(sessionRemainingSeconds)}
                </span>
              </div>
            </div>

            <div className="p-3 bg-amber-950/30 rounded-xl border border-amber-800/40 text-xs text-amber-200 flex items-start gap-2.5">
              <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
              <span>
                À l'expiration du jeton JWT, l'application nettoie automatiquement les données locales et redirige l'utilisateur vers la page de reconnexion avec le message « Session expirée ».
              </span>
            </div>

            <div className="pt-3 border-t border-[#27272A] flex items-center justify-between">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setSessionModalOpen(false)}
              >
                Fermer
              </Button>
              <Button
                variant="danger"
                size="sm"
                onClick={() => {
                  setSessionModalOpen(false);
                  expireSessionNow();
                }}
                icon={<LogOut className="w-3.5 h-3.5" />}
              >
                Simuler l'expiration immédiate
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </header>
  );
};
