import { describe, it, expect } from 'vitest';
import { formatCurrency, formatDate, getSaleStatusBadge, getSagaStatusBadge } from './formatters';

describe('formatters utility', () => {
  it('formats numeric amounts to EUR currency correctly', () => {
    const formatted = formatCurrency(1999.99);
    expect(formatted).toContain('1');
    expect(formatted).toContain('999');
    expect(formatted).toContain('€');

    expect(formatCurrency(null)).toBe('0,00 €');
    expect(formatCurrency(undefined)).toBe('0,00 €');
    expect(formatCurrency('abc')).toBe('0,00 €');
  });

  it('formats date strings', () => {
    expect(formatDate(null)).toBe('—');
    expect(formatDate(undefined)).toBe('—');

    const formatted = formatDate('2026-10-26T16:00:00Z');
    expect(formatted).not.toBe('—');
  });

  it('maps sale statuses to appropriate badges', () => {
    expect(getSaleStatusBadge('PENDING')).toEqual({ label: 'En attente', variant: 'warning' });
    expect(getSaleStatusBadge('CONFIRMED')).toEqual({ label: 'Confirmée', variant: 'info' });
    expect(getSaleStatusBadge('COMPLETED')).toEqual({ label: 'Terminée', variant: 'success' });
    expect(getSaleStatusBadge('CANCELLED')).toEqual({ label: 'Annulée', variant: 'danger' });
  });

  it('maps saga statuses to appropriate badges', () => {
    expect(getSagaStatusBadge('COMPLETED').variant).toBe('success');
    expect(getSagaStatusBadge('COMPENSATION_PENDING').variant).toBe('warning');
    expect(getSagaStatusBadge('COMPENSATED').variant).toBe('purple');
    expect(getSagaStatusBadge('FAILED_REQUIRES_ATTENTION').variant).toBe('danger');
  });
});
