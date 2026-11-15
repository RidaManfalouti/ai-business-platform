import React from 'react';
import { Loader2 } from 'lucide-react';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'outline' | 'ghost' | 'ai';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
  icon?: React.ReactNode;
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  disabled,
  icon,
  className = '',
  ...props
}) => {
  const baseStyles =
    'inline-flex items-center justify-center font-medium rounded-lg transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-[#09090B] disabled:opacity-40 disabled:cursor-not-allowed select-none active:scale-[0.98]';

  const sizeStyles = {
    sm: 'text-xs px-2.5 py-1.5 gap-1.5',
    md: 'text-sm px-4 py-2 gap-2',
    lg: 'text-base px-5 py-2.5 gap-2.5',
  }[size];

  const variantStyles = {
    primary:
      'bg-[#FACC15] text-[#09090B] font-semibold hover:bg-[#EAB308] active:bg-[#CA8A04] focus:ring-[#FACC15]/50 shadow-[0_0_15px_rgba(250,204,21,0.2)]',
    secondary:
      'bg-[#202024] text-[#FAFAFA] hover:bg-[#27272A] active:bg-[#202024] border border-[#27272A] hover:border-[#3F3F46] focus:ring-[#A1A1AA]/30 shadow-xs',
    ai:
      'bg-[#A855F7] text-white font-medium hover:bg-[#9333EA] active:bg-[#7E22CE] focus:ring-[#A855F7]/50 shadow-[0_0_20px_rgba(168,85,247,0.3)]',
    danger:
      'bg-[#EF4444] text-white font-medium hover:bg-[#DC2626] active:bg-[#B91C1C] focus:ring-[#EF4444]/40 shadow-xs',
    outline:
      'border border-[#27272A] bg-[#18181B] text-[#FAFAFA] hover:bg-[#202024] hover:border-[#3F3F46] focus:ring-[#FACC15]/30 shadow-xs',
    ghost:
      'bg-transparent text-[#A1A1AA] hover:text-[#FAFAFA] hover:bg-[#202024] focus:ring-[#27272A]',
  }[variant];

  return (
    <button
      className={`${baseStyles} ${sizeStyles} ${variantStyles} ${className}`}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <Loader2 className="w-4 h-4 animate-spin shrink-0" />
      ) : (
        icon && <span className="shrink-0">{icon}</span>
      )}
      <span>{children}</span>
    </button>
  );
};
