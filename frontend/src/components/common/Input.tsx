import React, { forwardRef } from 'react';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, helperText, leftIcon, rightIcon, className = '', id, required, ...props }, ref) => {
    const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

    return (
      <div className="w-full">
        {label && (
          <label htmlFor={inputId} className="block text-xs font-semibold text-[#A1A1AA] mb-1.5 uppercase tracking-wider">
            {label}
            {required && <span className="text-[#EF4444] ml-1">*</span>}
          </label>
        )}
        <div className="relative rounded-lg shadow-xs">
          {leftIcon && (
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-[#71717A]">
              {leftIcon}
            </div>
          )}
          <input
            id={inputId}
            ref={ref}
            required={required}
            className={`block w-full rounded-lg border text-sm transition-colors duration-150 placeholder:text-[#71717A] focus:outline-none focus:ring-2 focus:ring-offset-0 disabled:bg-[#131316] disabled:text-[#71717A] disabled:cursor-not-allowed ${
              leftIcon ? 'pl-9' : 'pl-3.5'
            } ${rightIcon ? 'pr-9' : 'pr-3.5'} py-2 bg-[#18181B] ${
              error
                ? 'border-[#EF4444] text-[#FAFAFA] focus:border-[#EF4444] focus:ring-[#EF4444]/20'
                : 'border-[#27272A] text-[#FAFAFA] hover:border-[#3F3F46] focus:border-[#FACC15] focus:ring-[#FACC15]/20'
            } ${className}`}
            {...props}
          />
          {rightIcon && (
            <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none text-[#71717A]">
              {rightIcon}
            </div>
          )}
        </div>
        {error && <p className="mt-1 text-xs text-[#EF4444] font-medium">{error}</p>}
        {!error && helperText && <p className="mt-1 text-xs text-[#71717A]">{helperText}</p>}
      </div>
    );
  }
);

Input.displayName = 'Input';
