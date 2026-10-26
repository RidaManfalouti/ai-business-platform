import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/common/Button';
import { Home, AlertTriangle } from 'lucide-react';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center p-6 text-center">
      <div className="p-4 bg-amber-50 text-amber-600 rounded-full mb-4 border border-amber-200">
        <AlertTriangle className="w-10 h-10" />
      </div>
      <h1 className="text-4xl font-extrabold text-slate-900 tracking-tight">404</h1>
      <h2 className="text-lg font-bold text-slate-700 mt-2">Page introuvable</h2>
      <p className="text-sm text-slate-500 max-w-md mt-1 mb-6">
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
