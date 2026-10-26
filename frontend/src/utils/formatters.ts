/**
 * Formats a numeric value into EUR currency string (e.g. "1 999,99 €")
 */
export function formatCurrency(amount: number | string | null | undefined): string {
  const numeric = typeof amount === 'string' ? parseFloat(amount) : amount;
  if (numeric == null || isNaN(numeric)) {
    return '0,00 €';
  }
  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(numeric);
}

/**
 * Formats an ISO date/timestamp into a readable French localized date string.
 */
export function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return '—';
  try {
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;
    return new Intl.DateTimeFormat('fr-FR', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(date);
  } catch {
    return dateStr;
  }
}

/**
 * Returns human-readable French label and badge style for SaleStatus.
 */
export function getSaleStatusBadge(status: string): { label: string; variant: 'warning' | 'info' | 'success' | 'danger' } {
  switch (status) {
    case 'PENDING':
      return { label: 'En attente', variant: 'warning' };
    case 'CONFIRMED':
      return { label: 'Confirmée', variant: 'info' };
    case 'COMPLETED':
      return { label: 'Terminée', variant: 'success' };
    case 'CANCELLED':
      return { label: 'Annulée', variant: 'danger' };
    default:
      return { label: status, variant: 'info' };
  }
}

/**
 * Returns human-readable label and style for SagaStatus.
 */
export function getSagaStatusBadge(status: string): { label: string; variant: 'warning' | 'info' | 'success' | 'danger' | 'purple' } {
  switch (status) {
    case 'STARTED':
      return { label: 'Démarrée', variant: 'info' };
    case 'STOCK_RESERVED':
      return { label: 'Stock réservé', variant: 'info' };
    case 'SALE_PERSISTED':
      return { label: 'Vente persistée', variant: 'info' };
    case 'COMPLETED':
      return { label: 'Complétée', variant: 'success' };
    case 'COMPENSATION_PENDING':
      return { label: 'Compensation en attente', variant: 'warning' };
    case 'COMPENSATING':
      return { label: 'Compensation en cours', variant: 'warning' };
    case 'COMPENSATED':
      return { label: 'Compensée', variant: 'purple' };
    case 'FAILED_REQUIRES_ATTENTION':
      return { label: 'Échec - Action requise', variant: 'danger' };
    default:
      return { label: status, variant: 'info' };
  }
}
