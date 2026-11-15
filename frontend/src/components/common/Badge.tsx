import React from 'react';

export interface BadgeProps {
  children: React.ReactNode;
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info' | 'purple' | 'yellow';
  size?: 'sm' | 'md';
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  variant = 'default',
  size = 'md',
  className = '',
}) => {
  const sizeStyles = {
    sm: 'text-[10px] px-2 py-0.5 font-medium',
    md: 'text-xs px-2.5 py-0.5 font-semibold',
  }[size];

  const variantStyles = {
    default: 'bg-[#202024] text-[#A1A1AA] border border-[#27272A]',
    success: 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20',
    warning: 'bg-amber-500/10 text-amber-400 border border-amber-500/20',
    danger: 'bg-rose-500/10 text-rose-400 border border-rose-500/20',
    info: 'bg-sky-500/10 text-sky-400 border border-sky-500/20',
    purple: 'bg-purple-500/15 text-purple-300 border border-purple-500/30 shadow-[0_0_10px_rgba(168,85,247,0.15)]',
    yellow: 'bg-[#FACC15]/15 text-[#FACC15] border border-[#FACC15]/30 shadow-[0_0_10px_rgba(250,204,21,0.15)]',
  }[variant];

  return (
    <span className={`inline-flex items-center rounded-full ${sizeStyles} ${variantStyles} ${className}`}>
      {children}
    </span>
  );
};
