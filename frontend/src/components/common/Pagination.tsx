import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from './Button';

export interface PaginationProps {
  currentPage: number; // 0-indexed
  totalPages: number;
  totalElements: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  isLoading?: boolean;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
  isLoading = false,
}) => {
  if (totalPages <= 1 && totalElements === 0) return null;

  const startItem = totalElements === 0 ? 0 : currentPage * pageSize + 1;
  const endItem = Math.min((currentPage + 1) * pageSize, totalElements);

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-4 px-4 py-3 bg-[#18181B] border-t border-[#27272A] rounded-b-xl text-xs text-[#A1A1AA]">
      <div>
        Affichage de <span className="font-semibold text-[#FAFAFA]">{startItem}</span> à{' '}
        <span className="font-semibold text-[#FAFAFA]">{endItem}</span> sur{' '}
        <span className="font-semibold text-[#FAFAFA]">{totalElements}</span> résultats
      </div>

      <div className="flex items-center gap-2">
        <span className="mr-2">
          Page <span className="font-semibold text-[#FAFAFA]">{currentPage + 1}</span> sur{' '}
          <span className="font-semibold text-[#FAFAFA]">{Math.max(1, totalPages)}</span>
        </span>

        <Button
          variant="outline"
          size="sm"
          disabled={currentPage === 0 || isLoading}
          onClick={() => onPageChange(currentPage - 1)}
          icon={<ChevronLeft className="w-4 h-4" />}
          aria-label="Page précédente"
        >
          Précédent
        </Button>

        <Button
          variant="outline"
          size="sm"
          disabled={currentPage >= totalPages - 1 || isLoading}
          onClick={() => onPageChange(currentPage + 1)}
          icon={<ChevronRight className="w-4 h-4" />}
          className="flex-row-reverse"
          aria-label="Page suivante"
        >
          Suivant
        </Button>
      </div>
    </div>
  );
};
