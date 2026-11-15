import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/common/Button';
import { Home, AlertTriangle } from 'lucide-react';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-[#09090B] flex flex-col items-center justify-center p-6 text-center relative overflow-hidden">
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-[#FACC15]/5 rounded-full blur-3xl pointer-events-none" />
      <div className="p-4 bg-[#FACC15]/10 text-[#FACC15] rounded-2xl mb-4 border border-[#FACC15]/20 shadow-[0_0_20px_rgba(250,204,21,0.2)]">
        <AlertTriangle className="w-10 h-10" />
      </div>
      <h1 className="text-5xl font-black text-[#FAFAFA] tracking-tight">404</h1>
      <h2 className="text-lg font-bold text-[#A1A1AA] mt-2">Page introuvable</h2>
      <p className="text-sm text-[#71717A] max-w-md mt-1 mb-6">
        L'URL demandée n'existe pas ou a été déplacée sur la plateforme.
      </p>
      <Link to="/">
        <Button variant="primary" icon={<Home className="w-4 h-4" />}>
          Retour au tableau de bord
        </Button>
      </Link>
    </div>
  );
};
