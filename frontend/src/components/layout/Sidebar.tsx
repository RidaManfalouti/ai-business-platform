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
  Sparkles,
} from 'lucide-react';

export interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  const { isAdmin } = useAuth();

  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-medium transition-all duration-150 ${
      isActive
        ? 'bg-[#FACC15] text-[#09090B] font-semibold shadow-[0_0_15px_rgba(250,204,21,0.25)]'
        : 'text-[#A1A1AA] hover:bg-[#202024] hover:text-[#FAFAFA]'
    }`;

  const aiNavLinkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-medium transition-all duration-150 ${
      isActive
        ? 'bg-[#A855F7] text-white font-semibold shadow-[0_0_20px_rgba(168,85,247,0.35)]'
        : 'text-[#D8B4FE] bg-[#A855F7]/10 border border-[#A855F7]/30 hover:bg-[#A855F7]/20 hover:text-white hover:border-[#A855F7]/50 shadow-[0_0_12px_rgba(168,85,247,0.12)]'
    }`;

  const sidebarContent = (
    <div className="flex h-full flex-col justify-between bg-[#18181B] text-[#FAFAFA] p-4 border-r border-[#27272A]">
      <div className="space-y-6">
        {/* Brand header */}
        <div className="flex items-center justify-between px-2 pt-1 pb-4 border-b border-[#27272A]">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#FACC15] text-[#09090B] font-black shadow-[0_0_15px_rgba(250,204,21,0.3)]">
              <Layers className="w-5 h-5" />
            </div>
            <div>
              <span className="text-sm font-bold tracking-tight text-[#FAFAFA] block">AI Business</span>
              <span className="text-[10px] font-bold text-[#FACC15] uppercase tracking-wider block">
                Platform
              </span>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 text-[#71717A] hover:text-[#FAFAFA] lg:hidden rounded-lg hover:bg-[#202024]"
            aria-label="Fermer le menu"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* AI Special Section */}
        <div className="space-y-1.5">
          <div className="px-3 pb-1 text-[10px] font-bold uppercase tracking-wider text-[#A855F7] flex items-center gap-1.5">
            <Sparkles className="w-3 h-3 text-[#A855F7]" />
            <span>Copilot & Intelligence</span>
          </div>
          <NavLink to="/ai" className={aiNavLinkClass} onClick={onClose}>
            <div className="flex items-center gap-2.5">
              <Sparkles className="w-4 h-4 shrink-0" />
              <span>Assistant IA Copilot</span>
            </div>
            <span className="text-[9px] uppercase px-1.5 py-0.5 rounded-full font-bold bg-[#A855F7]/30 text-white border border-[#A855F7]/40">
              PRO
            </span>
          </NavLink>
        </div>

        {/* Main navigation */}
        <nav className="space-y-1">
          <div className="px-3 pb-2 text-[10px] font-bold uppercase tracking-wider text-[#71717A]">
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
          <div className="pt-3 border-t border-[#27272A]">
            <div className="px-3 pb-2 text-[10px] font-bold uppercase tracking-wider text-[#FACC15]">
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
      <div className="rounded-xl bg-[#09090B] p-3 border border-[#27272A] text-[11px] text-[#A1A1AA]">
        <div className="flex items-center justify-between mb-1.5">
          <span className="font-semibold text-[#FAFAFA]">Microservices</span>
          <span className="inline-flex items-center gap-1.5 text-emerald-400 font-semibold text-[10px]">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            Connecté
          </span>
        </div>
        <div className="space-y-0.5 text-[10px] text-[#71717A]">
          <div>Passerelle API : Port 8080</div>
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
          className="fixed inset-0 z-40 bg-[#09090B]/80 backdrop-blur-sm lg:hidden"
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
      <aside className="hidden lg:flex lg:w-64 lg:flex-col lg:shrink-0">
        {sidebarContent}
      </aside>
    </>
  );
};
