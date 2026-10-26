import React from 'react';
import { useAuth } from '@/context/AuthContext';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { LogOut, Menu, User, Shield, Server } from 'lucide-react';

export interface HeaderProps {
  onToggleSidebar: () => void;
}

export const Header: React.FC<HeaderProps> = ({ onToggleSidebar }) => {
  const { user, isAdmin, logout } = useAuth();

  return (
    <header className="sticky top-0 z-30 flex h-16 shrink-0 items-center justify-between border-b border-slate-200 bg-white/95 px-4 sm:px-6 backdrop-blur-xs">
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onToggleSidebar}
          className="p-2 text-slate-500 rounded-lg hover:bg-slate-100 lg:hidden focus:outline-none"
          aria-label="Ouvrir le menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        {/* Gateway connection pill */}
        <div className="hidden sm:flex items-center gap-2 px-2.5 py-1 rounded-full bg-slate-100 text-slate-600 text-xs border border-slate-200">
          <Server className="w-3.5 h-3.5 text-emerald-600" />
          <span>Passerelle API</span>
          <span className="inline-block w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
        </div>
      </div>

      <div className="flex items-center gap-3 sm:gap-4">
        {user && (
          <div className="flex items-center gap-2.5 text-right">
            <div className="hidden md:block">
              <div className="text-xs font-bold text-slate-800">{user.username}</div>
              <div className="text-[11px] text-slate-400 max-w-[160px] truncate">{user.email}</div>
            </div>

            <div className="flex items-center gap-1.5">
              <Badge variant={isAdmin ? 'purple' : 'info'} size="sm" className="gap-1">
                {isAdmin ? <Shield className="w-3 h-3" /> : <User className="w-3 h-3" />}
                {isAdmin ? 'ADMIN' : 'USER'}
              </Badge>
            </div>
          </div>
        )}

        <div className="h-6 w-px bg-slate-200" />

        <Button
          variant="ghost"
          size="sm"
          onClick={logout}
          icon={<LogOut className="w-4 h-4 text-slate-500" />}
          className="text-slate-600 hover:text-rose-600"
          title="Se déconnecter"
        >
          <span className="hidden sm:inline">Déconnexion</span>
        </Button>
      </div>
    </header>
  );
};
