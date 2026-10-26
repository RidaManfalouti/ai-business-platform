import { DecodedJwt, UserRole } from '@/types/auth';

/**
 * Safely decodes a JWT token payload without external libraries.
 * Note: Client-side decoding is strictly for UI display and route gating.
 * The backend remains the sole cryptographic authority.
 */
export function decodeJwt(token: string): DecodedJwt | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }

    // Replace URL-safe chars and pad with '='
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );

    return JSON.parse(jsonPayload) as DecodedJwt;
  } catch {
    return null;
  }
}

/**
 * Checks if a JWT token is expired based on its `exp` claim.
 */
export function isTokenExpired(token: string): boolean {
  const decoded = decodeJwt(token);
  if (!decoded || !decoded.exp) {
    return true;
  }
  const currentTimeSeconds = Math.floor(Date.now() / 1000);
  // Add a 10-second safety margin
  return decoded.exp <= currentTimeSeconds + 10;
}

/**
 * Extracts normalized role ('ADMIN' | 'USER') from token claims.
 */
export function extractRoleFromToken(token: string): UserRole {
  const decoded = decodeJwt(token);
  if (!decoded || !decoded.roles || !Array.isArray(decoded.roles)) {
    return 'USER';
  }

  const hasAdmin = decoded.roles.some((r) =>
    r === 'ROLE_ADMIN' || r === 'ADMIN'
  );
  return hasAdmin ? 'ADMIN' : 'USER';
}
