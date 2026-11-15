import React from 'react';

export interface TableProps {
  children: React.ReactNode;
  className?: string;
}

export const Table: React.FC<TableProps> = ({ children, className = '' }) => {
  return (
    <div className={`overflow-x-auto rounded-xl border border-[#27272A] bg-[#18181B] shadow-xs ${className}`}>
      <table className="w-full text-left text-sm text-[#A1A1AA] divide-y divide-[#27272A]">
        {children}
      </table>
    </div>
  );
};

export const TableHead: React.FC<{ children: React.ReactNode; className?: string }> = ({
  children,
  className = '',
}) => {
  return <thead className={`bg-[#131316] text-[11px] font-semibold text-[#A1A1AA] uppercase tracking-wider border-b border-[#27272A] ${className}`}>{children}</thead>;
};

export const TableBody: React.FC<{ children: React.ReactNode; className?: string }> = ({
  children,
  className = '',
}) => {
  return <tbody className={`divide-y divide-[#27272A] bg-[#18181B] ${className}`}>{children}</tbody>;
};

export const TableRow: React.FC<{ children: React.ReactNode; className?: string; onClick?: () => void }> = ({
  children,
  className = '',
  onClick,
}) => {
  return (
    <tr
      onClick={onClick}
      className={`transition-colors duration-100 hover:bg-[#202024] ${onClick ? 'cursor-pointer' : ''} ${className}`}
    >
      {children}
    </tr>
  );
};

export const TableHeaderCell: React.FC<
  React.ThHTMLAttributes<HTMLTableCellElement> & { children: React.ReactNode; className?: string }
> = ({ children, className = '', ...props }) => {
  return (
    <th scope="col" className={`px-4 py-3.5 font-semibold text-[#A1A1AA] ${className}`} {...props}>
      {children}
    </th>
  );
};

export const TableCell: React.FC<
  React.TdHTMLAttributes<HTMLTableCellElement> & { children: React.ReactNode; className?: string }
> = ({ children, className = '', ...props }) => {
  return (
    <td className={`px-4 py-3.5 whitespace-nowrap text-[#FAFAFA] ${className}`} {...props}>
      {children}
    </td>
  );
};
