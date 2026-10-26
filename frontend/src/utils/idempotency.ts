/**
 * Generates an idempotency key / sale reference code
 * formatted as: CMD-YYYYMMDD-XXXXXX
 */
export function generateSaleReference(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  
  // 6 random uppercase alphanumeric characters
  const rand = Math.random().toString(36).substring(2, 8).toUpperCase();
  return `CMD-${year}${month}${day}-${rand}`;
}

/**
 * Generates a standard UUID v4 or random hex string for request correlation tracking
 */
export function generateCorrelationId(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'corr-' + Math.random().toString(36).substring(2, 15);
}
