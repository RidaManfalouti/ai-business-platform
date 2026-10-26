import React from 'react';

export interface TableProps {
  children: React.ReactNode;
  className?: string;
}

export const Table: React.FC<TableProps> = ({ children, className = '' }) => {
  return (
    <div className={`overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-xs ${className}`}>
      <table className="w-full text-left text-sm text-slate-600 divide-y divide-slate-200">
        {children}
      </table>
    </div>
  );
};

export const TableHead: React.FC<{ children: React.ReactNode; className?: string }> = ({
  children,
  className = '',
}) => {
  return <thead className={`bg-slate-50 text-xs font-semibold text-slate-600 uppercase tracking-wider ${className}`}>{children}</thead>;
};

export const TableBody: React.FC<{ children: React.ReactNode; className?: string }> = ({
  children,
  className = '',
}) => {
  return <tbody className={`divide-y divide-slate-100 bg-white ${className}`}>{children}</tbody>;
};

export const TableRow: React.FC<{ children: React.ReactNode; className?: string; onClick?: () => void }> = ({
  children,
  className = '',
  onClick,
}) => {
  return (
    <tr
      onClick={onClick}
      className={`transition-colors hover:bg-slate-50/70 ${onClick ? 'cursor-pointer' : ''} ${className}`}
    >
      {children}
    </tr>
  );
};

export const TableHeaderCell: React.FC<
  React.ThHTMLAttributes<HTMLTableCellElement> & { children: React.ReactNode; className?: string }
> = ({ children, className = '', ...props }) => {
  return (
    <th scope="col" className={`px-4 py-3.5 ${className}`} {...props}>
      {children}
    </th>
  );
};

export const TableCell: React.FC<
  React.TdHTMLAttributes<HTMLTableCellElement> & { children: React.ReactNode; className?: string }
> = ({ children, className = '', ...props }) => {
  return (
    <td className={`px-4 py-3.5 whitespace-nowrap text-slate-700 ${className}`} {...props}>
      {children}
    </td>
  );
};
