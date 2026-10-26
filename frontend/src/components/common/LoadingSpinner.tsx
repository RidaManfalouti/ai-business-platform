import React from 'react';
import { Loader2 } from 'lucide-react';

export interface LoadingSpinnerProps {
  label?: string;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  label = 'Chargement des données...',
  size = 'md',
  className = '',
}) => {
  const sizeStyles = {
    sm: 'w-4 h-4',
    md: 'w-8 h-8',
    lg: 'w-12 h-12',
  }[size];

  return (
    <div
      className={`flex flex-col items-center justify-center p-8 sm:p-12 text-slate-500 ${className}`}
      role="status"
    >
      <Loader2 className={`${sizeStyles} animate-spin text-emerald-600 mb-2.5`} />
      {label && <p className="text-xs font-medium text-slate-500">{label}</p>}
      <span className="sr-only">Chargement...</span>
    </div>
  );
};
