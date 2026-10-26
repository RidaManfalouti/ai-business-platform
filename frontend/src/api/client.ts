import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiErrorResponse, ParsedApiError } from '@/types/api';
import { generateCorrelationId } from '@/utils/idempotency';

export const TOKEN_STORAGE_KEY = 'ai_platform_token';
export const USER_STORAGE_KEY = 'ai_platform_user';

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const timeout = Number(import.meta.env.VITE_API_TIMEOUT_MS) || 15000;

export const apiClient = axios.create({
  baseURL,
  timeout,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

// Request interceptor: attach Bearer token and correlation ID
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    try {
      const token = localStorage.getItem(TOKEN_STORAGE_KEY);
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch {
      // localStorage may be disabled in restricted environments
    }

    if (config.headers && !config.headers['X-Correlation-Id']) {
      config.headers['X-Correlation-Id'] = generateCorrelationId();
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: handle 401 unauthorized & error parsing
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    if (error.response?.status === 401) {
      try {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        localStorage.removeItem(USER_STORAGE_KEY);
      } catch {
        // ignore
      }

      // Avoid looping if already on login or auth endpoint
      if (
        typeof window !== 'undefined' &&
        !window.location.pathname.startsWith('/login') &&
        !error.config?.url?.includes('/api/auth/')
      ) {
        window.location.href = '/login?expired=true';
      }
    }

    return Promise.reject(parseApiError(error));
  }
);

/**
 * Normalizes an unknown error or Axios error into a standard ParsedApiError.
 */
export function parseApiError(error: unknown): ParsedApiError {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status || (error.code === 'ECONNABORTED' ? 408 : 500);
    const data = error.response?.data as ApiErrorResponse | undefined;

    // Detect Gateway 503 fallback
    const isGatewayUnavailable =
      status === 503 ||
      data?.error === 'SERVICE_UNAVAILABLE' ||
      error.message.includes('Network Error');

    let message = 'Une erreur réseau est survenue. Veuillez vérifier votre connexion.';

    if (data?.message) {
      message = data.message;
    } else if (error.code === 'ECONNABORTED') {
      message = 'Le délai d’attente de la requête a été dépassé (timeout).';
    } else if (status === 403) {
      message = 'Accès interdit. Vos privilèges sont insuffisants pour exécuter cette opération.';
    } else if (status === 404) {
      message = 'La ressource demandée est introuvable.';
    } else if (status === 409) {
      message = data?.message || 'Conflit de données ou ressource déjà existante.';
    } else if (isGatewayUnavailable) {
      message = data?.message || 'Le service est actuellement indisponible ou en cours de démarrage.';
    }

    return {
      status,
      code: data?.error || (status === 401 ? 'UNAUTHORIZED' : 'UNKNOWN_ERROR'),
      message,
      fieldErrors: data?.messages,
      isGatewayUnavailable,
    };
  }

  if (error instanceof Error) {
    return {
      status: 500,
      code: 'CLIENT_ERROR',
      message: error.message,
    };
  }

  return {
    status: 500,
    code: 'UNKNOWN',
    message: 'Une erreur imprévue est survenue.',
  };
}
