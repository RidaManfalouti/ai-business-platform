import { describe, it, expect } from 'vitest';
import { generateSaleReference, generateCorrelationId } from './idempotency';

describe('idempotency utility', () => {
  it('generates a sale reference with CMD- prefix and date', () => {
    const ref = generateSaleReference();
    expect(ref).toMatch(/^CMD-\d{8}-[A-Z0-9]{6}$/);
  });

  it('generates unique references across multiple calls', () => {
    const ref1 = generateSaleReference();
    const ref2 = generateSaleReference();
    expect(ref1).not.toBe(ref2);
  });

  it('generates non-empty correlation IDs', () => {
    const corrId = generateCorrelationId();
    expect(corrId.length).toBeGreaterThan(5);
  });
});
