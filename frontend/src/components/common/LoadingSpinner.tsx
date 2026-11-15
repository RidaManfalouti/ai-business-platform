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
      className={`flex flex-col items-center justify-center p-8 sm:p-12 text-[#A1A1AA] ${className}`}
      role="status"
    >
      <Loader2 className={`${sizeStyles} animate-spin text-[#FACC15] mb-2.5 drop-shadow-[0_0_8px_rgba(250,204,21,0.4)]`} />
      {label && <p className="text-xs font-medium text-[#A1A1AA]">{label}</p>}
      <span className="sr-only">Chargement...</span>
    </div>
  );
};
