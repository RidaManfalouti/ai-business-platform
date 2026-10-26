import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import {
  LayoutDashboard,
  Package,
  Users,
  ShoppingCart,
  GitBranch,
  ShieldCheck,
  X,
  Layers,
} from 'lucide-react';

export interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  const { isAdmin } = useAuth();

  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-all ${
      isActive
        ? 'bg-emerald-600 text-white shadow-sm'
        : 'text-slate-300 hover:bg-slate-800/80 hover:text-white'
    }`;

  const sidebarContent = (
    <div className="flex h-full flex-col justify-between bg-slate-900 text-slate-100 p-4">
      <div className="space-y-6">
        {/* Brand header */}
        <div className="flex items-center justify-between px-2 pt-1 pb-3 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-500 text-slate-950 font-bold shadow-md">
              <Layers className="w-5 h-5" />
            </div>
            <div>
              <span className="text-sm font-bold tracking-tight text-white block">AI Business</span>
              <span className="text-[10px] font-semibold text-emerald-400 uppercase tracking-wider block">
                Platform
              </span>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 text-slate-400 hover:text-white lg:hidden rounded-lg hover:bg-slate-800"
            aria-label="Fermer le menu"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Main navigation */}
        <nav className="space-y-1">
          <div className="px-3 pb-2 text-[10px] font-bold uppercase tracking-wider text-slate-500">
            Gestion Opérationnelle
          </div>
          <NavLink to="/" end className={navLinkClass} onClick={onClose}>
            <LayoutDashboard className="w-4 h-4 shrink-0" />
            <span>Tableau de bord</span>
          </NavLink>
          <NavLink to="/products" className={navLinkClass} onClick={onClose}>
            <Package className="w-4 h-4 shrink-0" />
            <span>Produits & Stock</span>
          </NavLink>
          <NavLink to="/customers" className={navLinkClass} onClick={onClose}>
            <Users className="w-4 h-4 shrink-0" />
            <span>Clients</span>
          </NavLink>
          <NavLink to="/sales" className={navLinkClass} onClick={onClose}>
            <ShoppingCart className="w-4 h-4 shrink-0" />
            <span>Ventes & Commandes</span>
          </NavLink>
        </nav>

        {/* Admin Navigation */}
        {isAdmin && (
          <div className="pt-3 border-t border-slate-800">
            <div className="px-3 pb-2 text-[10px] font-bold uppercase tracking-wider text-amber-400">
              Administration (Admin)
            </div>
            <nav className="space-y-1">
              <NavLink to="/admin/sagas" className={navLinkClass} onClick={onClose}>
                <GitBranch className="w-4 h-4 shrink-0" />
                <span>Sagas & Réconciliation</span>
              </NavLink>
              <NavLink to="/admin/users" className={navLinkClass} onClick={onClose}>
                <ShieldCheck className="w-4 h-4 shrink-0" />
                <span>Rôles & Utilisateurs</span>
              </NavLink>
            </nav>
          </div>
        )}
      </div>

      {/* Footer architecture status */}
      <div className="rounded-lg bg-slate-950/60 p-3 border border-slate-800/80 text-[11px] text-slate-400">
        <div className="flex items-center justify-between mb-1.5">
          <span className="font-semibold text-slate-300">Microservices</span>
          <span className="inline-flex items-center gap-1 text-emerald-400 font-semibold">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
            Actifs
          </span>
        </div>
        <div className="space-y-0.5 text-slate-500">
          <div>Port 8080 (API Gateway)</div>
          <div>PostgreSQL 16 & Redis 7</div>
        </div>
      </div>
    </div>
  );

  return (
    <>
      {/* Mobile Drawer Backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-950/60 backdrop-blur-xs lg:hidden"
          onClick={onClose}
        />
      )}

      {/* Mobile Drawer */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 transform transition-transform duration-200 ease-in-out lg:hidden ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {sidebarContent}
      </aside>

      {/* Desktop Static Sidebar */}
      <aside className="hidden lg:flex lg:w-64 lg:flex-col lg:shrink-0 border-r border-slate-800">
        {sidebarContent}
      </aside>
    </>
  );
};
