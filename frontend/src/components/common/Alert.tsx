import React from 'react';
import { AlertCircle, CheckCircle2, Info, AlertTriangle, X } from 'lucide-react';

export interface AlertProps {
  title?: string;
  children: React.ReactNode;
  variant?: 'info' | 'success' | 'warning' | 'error';
  onDismiss?: () => void;
  className?: string;
}

export const Alert: React.FC<AlertProps> = ({
  title,
  children,
  variant = 'info',
  onDismiss,
  className = '',
}) => {
  const styles = {
    info: {
      container: 'bg-[#18181B] border-[#27272A] border-l-4 border-l-sky-500 text-[#FAFAFA]',
      icon: <Info className="w-5 h-5 text-sky-400 shrink-0" />,
    },
    success: {
      container: 'bg-[#18181B] border-[#27272A] border-l-4 border-l-emerald-500 text-[#FAFAFA]',
      icon: <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />,
    },
    warning: {
      container: 'bg-[#18181B] border-[#27272A] border-l-4 border-l-[#FACC15] text-[#FAFAFA]',
      icon: <AlertTriangle className="w-5 h-5 text-[#FACC15] shrink-0" />,
    },
    error: {
      container: 'bg-[#18181B] border-[#27272A] border-l-4 border-l-[#EF4444] text-[#FAFAFA]',
      icon: <AlertCircle className="w-5 h-5 text-[#EF4444] shrink-0" />,
    },
  }[variant];

  return (
    <div
      role="alert"
      className={`flex items-start gap-3 p-3.5 rounded-lg border text-sm shadow-xs ${styles.container} ${className}`}
    >
      <div className="mt-0.5">{styles.icon}</div>
      <div className="flex-1">
        {title && <h5 className="font-semibold text-sm mb-0.5 text-[#FAFAFA]">{title}</h5>}
        <div className="leading-relaxed text-[#A1A1AA] text-xs">{children}</div>
      </div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          className="p-1 rounded-md text-[#71717A] hover:text-[#FAFAFA] hover:bg-[#202024] transition-colors focus:outline-none"
          aria-label="Fermer"
        >
          <X className="w-4 h-4" />
        </button>
      )}
    </div>
  );
};
