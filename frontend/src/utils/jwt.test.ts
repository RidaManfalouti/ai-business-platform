import { describe, it, expect } from 'vitest';
import { decodeJwt, isTokenExpired, extractRoleFromToken } from './jwt';

describe('jwt utility', () => {
  // A test helper creating simple base64url encoded JWTs
  const createMockJwt = (payload: object) => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body = btoa(JSON.stringify(payload))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
    return `${header}.${body}.mockSignature`;
  };

  it('should return null for malformed tokens', () => {
    expect(decodeJwt('')).toBeNull();
    expect(decodeJwt('invalid.token')).toBeNull();
    expect(decodeJwt('a.b.c.d')).toBeNull();
  });

  it('should decode a valid JWT payload correctly', () => {
    const payload = { sub: 'test@example.com', roles: ['ROLE_USER'], iat: 1000, exp: 2000 };
    const token = createMockJwt(payload);
    const decoded = decodeJwt(token);

    expect(decoded).not.toBeNull();
    expect(decoded?.sub).toBe('test@example.com');
    expect(decoded?.roles).toEqual(['ROLE_USER']);
  });

  it('should accurately detect expired and active tokens', () => {
    const nowSec = Math.floor(Date.now() / 1000);

    const expiredToken = createMockJwt({ exp: nowSec - 100 });
    const futureToken = createMockJwt({ exp: nowSec + 3600 });

    expect(isTokenExpired(expiredToken)).toBe(true);
    expect(isTokenExpired(futureToken)).toBe(false);
  });

  it('should extract ADMIN role when present in claims', () => {
    const adminToken = createMockJwt({ roles: ['ROLE_ADMIN'] });
    const userToken = createMockJwt({ roles: ['ROLE_USER'] });
    const emptyToken = createMockJwt({});

    expect(extractRoleFromToken(adminToken)).toBe('ADMIN');
    expect(extractRoleFromToken(userToken)).toBe('USER');
    expect(extractRoleFromToken(emptyToken)).toBe('USER');
  });
});
