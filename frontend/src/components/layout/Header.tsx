import React from 'react';
import { useAuth } from '@/context/AuthContext';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { TenantSwitcher } from '@/components/layout/TenantSwitcher';
import { LogOut, Menu, User, Shield, Server } from 'lucide-react';

export interface HeaderProps {
  onToggleSidebar: () => void;
}

export const Header: React.FC<HeaderProps> = ({ onToggleSidebar }) => {
  const { user, isAdmin, logout } = useAuth();

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
    </header>
  );
};
