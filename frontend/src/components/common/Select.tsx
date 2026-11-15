import React, { forwardRef } from 'react';

export interface SelectOption {
  value: string | number;
  label: string;
}

export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  options: SelectOption[];
  error?: string;
  helperText?: string;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, options, error, helperText, className = '', id, required, ...props }, ref) => {
    const selectId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

    return (
      <div className="w-full">
        {label && (
          <label htmlFor={selectId} className="block text-xs font-semibold text-[#A1A1AA] mb-1.5 uppercase tracking-wider">
            {label}
            {required && <span className="text-[#EF4444] ml-1">*</span>}
          </label>
        )}
        <select
          id={selectId}
          ref={ref}
          required={required}
          className={`block w-full rounded-lg border bg-[#18181B] text-sm transition-colors duration-150 focus:outline-none focus:ring-2 focus:ring-offset-0 disabled:bg-[#131316] disabled:text-[#71717A] disabled:cursor-not-allowed px-3.5 py-2 ${
            error
              ? 'border-[#EF4444] text-[#FAFAFA] focus:border-[#EF4444] focus:ring-[#EF4444]/20'
              : 'border-[#27272A] text-[#FAFAFA] hover:border-[#3F3F46] focus:border-[#FACC15] focus:ring-[#FACC15]/20'
          } ${className}`}
          {...props}
        >
          {options.map((opt) => (
            <option key={opt.value} value={opt.value} className="bg-[#18181B] text-[#FAFAFA]">
              {opt.label}
            </option>
          ))}
        </select>
        {error && <p className="mt-1 text-xs text-[#EF4444] font-medium">{error}</p>}
        {!error && helperText && <p className="mt-1 text-xs text-[#71717A]">{helperText}</p>}
      </div>
    );
  }
);

Select.displayName = 'Select';
