import React, { useEffect } from 'react';
import { X } from 'lucide-react';

export interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  description?: string;
  children: React.ReactNode;
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl';
}

export const Modal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  title,
  description,
  children,
  maxWidth = 'md',
}) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      window.addEventListener('keydown', handleKeyDown);
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const widthClasses = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
    '2xl': 'max-w-2xl',
  }[maxWidth];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto"
      role="dialog"
      aria-modal="true"
    >
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-[#09090B]/80 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />

      {/* Dialog card */}
      <div
        className={`relative w-full ${widthClasses} bg-[#18181B] rounded-2xl shadow-2xl border border-[#27272A] shadow-[0_25px_60px_rgba(0,0,0,0.9)] z-10 overflow-hidden transform transition-all my-8`}
      >
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#27272A] bg-[#202024]/40">
          <div>
            {title && <h3 className="text-base font-bold text-[#FAFAFA]">{title}</h3>}
            {description && <p className="text-xs text-[#A1A1AA] mt-0.5">{description}</p>}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 text-[#71717A] hover:text-[#FAFAFA] rounded-lg hover:bg-[#202024] transition-colors"
            aria-label="Fermer la boîte de dialogue"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 max-h-[calc(85vh-8rem)] overflow-y-auto text-[#FAFAFA]">{children}</div>
      </div>
    </div>
  );
};
