import React from 'react';
import { PackageOpen } from 'lucide-react';
import { Button } from './Button';

export interface EmptyStateProps {
  title: string;
  description: string;
  icon?: React.ReactNode;
  actionLabel?: string;
  onAction?: () => void;
  className?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title,
  description,
  icon,
  actionLabel,
  onAction,
  className = '',
}) => {
  return (
    <div
      className={`flex flex-col items-center justify-center p-8 sm:p-12 text-center bg-[#18181B]/50 rounded-xl border border-dashed border-[#27272A] ${className}`}
    >
      <div className="p-3 bg-[#202024] text-[#A1A1AA] rounded-2xl mb-3.5 border border-[#27272A] shadow-inner">
        {icon || <PackageOpen className="w-8 h-8 text-[#71717A]" />}
      </div>
      <h3 className="text-base font-semibold text-[#FAFAFA] mb-1">{title}</h3>
      <p className="text-sm text-[#A1A1AA] max-w-sm mb-5 leading-relaxed">{description}</p>
      {actionLabel && onAction && (
        <Button variant="primary" size="sm" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  );
};
