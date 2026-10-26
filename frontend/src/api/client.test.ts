import { describe, it, expect } from 'vitest';
import { parseApiError } from './client';
import { AxiosError, AxiosResponse } from 'axios';

describe('client API error parser', () => {
  it('parses structured backend validation errors', () => {
    const errorResponse = {
      error: 'VALIDATION_ERROR',
      messages: {
        email: 'Format d’adresse email invalide.',
        password: 'Le mot de passe doit comporter au minimum 8 caractères.',
      },
    };

    const axiosError = new AxiosError('Bad Request', 'ERR_BAD_REQUEST', undefined, undefined, {
      status: 400,
      data: errorResponse,
      headers: {},
      statusText: 'Bad Request',
      config: {} as any,
    } as AxiosResponse);

    const parsed = parseApiError(axiosError);
    expect(parsed.status).toBe(400);
    expect(parsed.code).toBe('VALIDATION_ERROR');
    expect(parsed.fieldErrors?.email).toBe('Format d’adresse email invalide.');
    expect(parsed.fieldErrors?.password).toBe(
      'Le mot de passe doit comporter au minimum 8 caractères.'
    );
  });

  it('detects gateway 503 service unavailable response', () => {
    const gatewayError = {
      timestamp: '2026-10-26T16:00:00Z',
      status: 503,
      error: 'SERVICE_UNAVAILABLE',
      message: 'Le service demandé n’est pas encore disponible.',
      path: '/api/analytics/report',
    };

    const axiosError = new AxiosError(
      'Service Unavailable',
      'ERR_BAD_RESPONSE',
      undefined,
      undefined,
      {
        status: 503,
        data: gatewayError,
        headers: {},
        statusText: 'Service Unavailable',
        config: {} as any,
      } as AxiosResponse
    );

    const parsed = parseApiError(axiosError);
    expect(parsed.status).toBe(503);
    expect(parsed.isGatewayUnavailable).toBe(true);
    expect(parsed.message).toBe('Le service demandé n’est pas encore disponible.');
  });

  it('handles general Error instances', () => {
    const error = new Error('Custom exception message');
    const parsed = parseApiError(error);
    expect(parsed.status).toBe(500);
    expect(parsed.message).toBe('Custom exception message');
    expect(parsed.code).toBe('CLIENT_ERROR');
  });
});
