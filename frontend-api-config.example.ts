/**
 * Configuration de l'API Backend CMK ERP
 *
 * Ce fichier peut être utilisé comme référence pour configurer votre client API frontend.
 * Copiez ce fichier dans votre projet frontend et adaptez-le selon votre stack (Axios, Fetch, etc.)
 */

// ==========================================
// CONFIGURATION BASE URL
// ==========================================

/**
 * URL de base du backend API
 *
 * Développement : http://localhost:8984/cmkerp-gateway
 * Production : https://api.cmkerp.com/cmkerp-gateway (à configurer)
 */
export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL ||
  process.env.REACT_APP_API_URL ||
  'http://localhost:8984/cmkerp-gateway';

/**
 * Version de l'API (actuellement v1)
 */
export const API_VERSION = 'v1';

/**
 * Base URL complète de l'API v1
 */
export const API_V1_BASE = `${API_BASE_URL}/api/${API_VERSION}`;

// ==========================================
// ENDPOINTS PAR RESSOURCE
// ==========================================

export const API_ENDPOINTS = {
  // Authentification (PUBLIC)
  AUTH: {
    LOGIN: `${API_V1_BASE}/auth/login`,
    REFRESH: `${API_V1_BASE}/auth/refresh`, // Si implémenté
  },

  // Utilisateurs
  USERS: {
    BASE: `${API_V1_BASE}/users`,
    BY_ID: (id: number | string) => `${API_V1_BASE}/users/${id}`,
    PHARMACIES: (id: number | string) => `${API_V1_BASE}/users/${id}/pharmacies`,
  },

  // Rôles
  ROLES: {
    BASE: `${API_V1_BASE}/roles`,
    BY_ID: (id: number | string) => `${API_V1_BASE}/roles/${id}`,
  },

  // Permissions
  PERMISSIONS: {
    BASE: `${API_V1_BASE}/permissions`,
    BY_ID: (id: number | string) => `${API_V1_BASE}/permissions/${id}`,
  },

  // Sites
  SITES: {
    BASE: `${API_V1_BASE}/sites`,
    BY_ID: (id: number | string) => `${API_V1_BASE}/sites/${id}`,
  },

  // Pharmacies
  PHARMACIES: {
    BASE: `${API_V1_BASE}/pharmacies`,
    BY_ID: (id: number | string) => `${API_V1_BASE}/pharmacies/${id}`,
  },

  // Dashboard
  DASHBOARD: {
    PHARMACIES: `${API_V1_BASE}/dashboard/pharmacies`,
    PHARMACIES_STOCK: `${API_V1_BASE}/dashboard/pharmacies/stock`,
    PHARMACIES_RUPTURES: `${API_V1_BASE}/dashboard/pharmacies/ruptures`,
  },

  // Notifications
  NOTIFICATIONS: {
    BASE: `${API_V1_BASE}/notifications`,
    BY_ID: (id: number | string) => `${API_V1_BASE}/notifications/${id}`,
    MARK_READ: (id: number | string) => `${API_V1_BASE}/notifications/${id}/read`,
  },

  // Health
  HEALTH: `${API_V1_BASE}/health`,

  // Server-Sent Events (SSE)
  SSE: {
    NOTIFICATIONS: `${API_V1_BASE}/sse/notifications`,
    AUDIT: `${API_V1_BASE}/sse/audit`,
  },
} as const;

// ==========================================
// SWAGGER / OPENAPI
// ==========================================

export const SWAGGER_UI_URL = `${API_BASE_URL}/swagger-ui.html`;
export const OPENAPI_JSON_URL = `${API_BASE_URL}/v3/api-docs/api-v1`;

// ==========================================
// TYPES D'ERREUR
// ==========================================

export interface ApiError {
  message: string;
  error?: string;
  status: number;
  timestamp?: string;
  path?: string;
}

// ==========================================
// TYPES DE RÉPONSE
// ==========================================

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: {
    id: number;
    username: string;
    email?: string;
    permissions: string[];
  };
}

export interface LoginRequest {
  username: string;
  password: string;
}

// ==========================================
// UTILITAIRES
// ==========================================

/**
 * Récupère le token d'authentification depuis le stockage
 */
export const getAuthToken = (): string | null => {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
};

/**
 * Sauvegarde le token d'authentification
 */
export const saveAuthToken = (token: string, persistent: boolean = true): void => {
  if (typeof window === 'undefined') return;
  if (persistent) {
    localStorage.setItem('accessToken', token);
  } else {
    sessionStorage.setItem('accessToken', token);
  }
};

/**
 * Supprime le token d'authentification
 */
export const clearAuthToken = (): void => {
  if (typeof window === 'undefined') return;
  localStorage.removeItem('accessToken');
  sessionStorage.removeItem('accessToken');
};

/**
 * Vérifie si l'utilisateur est authentifié
 */
export const isAuthenticated = (): boolean => {
  return getAuthToken() !== null;
};

/**
 * Construit les headers pour une requête authentifiée
 */
export const getAuthHeaders = (): Record<string, string> => {
  const token = getAuthToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return headers;
};

// ==========================================
// EXEMPLE D'UTILISATION AVEC FETCH
// ==========================================

/**
 * Exemple de fonction pour faire un appel API avec Fetch
 */
export async function apiFetch<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const url = endpoint.startsWith('http') ? endpoint : `${API_V1_BASE}${endpoint}`;

  const response = await fetch(url, {
    ...options,
    headers: {
      ...getAuthHeaders(),
      ...options.headers,
    },
  });

  if (!response.ok) {
    const error: ApiError = await response.json().catch(() => ({
      message: `HTTP ${response.status}: ${response.statusText}`,
      status: response.status,
    }));

    // Gérer les erreurs d'authentification
    if (response.status === 401) {
      clearAuthToken();
      // Rediriger vers login si dans un navigateur
      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }
    }

    throw error;
  }

  return response.json();
}

// ==========================================
// EXEMPLE D'UTILISATION AVEC AXIOS
// ==========================================

/**
 * Exemple de configuration Axios (si vous utilisez Axios)
 *
 * import axios from 'axios';
 *
 * export const apiClient = axios.create({
 *   baseURL: API_V1_BASE,
 *   headers: {
 *     'Content-Type': 'application/json',
 *   },
 * });
 *
 * // Intercepteur pour ajouter le token
 * apiClient.interceptors.request.use((config) => {
 *   const token = getAuthToken();
 *   if (token) {
 *     config.headers.Authorization = `Bearer ${token}`;
 *   }
 *   return config;
 * });
 *
 * // Intercepteur pour gérer les erreurs
 * apiClient.interceptors.response.use(
 *   (response) => response,
 *   (error) => {
 *     if (error.response?.status === 401) {
 *       clearAuthToken();
 *       window.location.href = '/login';
 *     }
 *     return Promise.reject(error);
 *   }
 * );
 */



