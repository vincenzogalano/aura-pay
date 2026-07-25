import axios from 'axios';

// API Client configurato per puntare all'API Gateway (porta 8080)
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Interceptor per iniettare l'header X-Correlation-ID ed eventuale API Key
apiClient.interceptors.request.use((config) => {
  const correlationId = `corr_fe_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
  config.headers['X-Correlation-ID'] = correlationId;

  // Recupera API Key salvata nel localStorage/context se disponibile
  const activeKey = localStorage.getItem('aurapay_active_api_key');
  if (activeKey) {
    config.headers['X-Api-Key'] = activeKey;
  }

  return config;
}, (error) => {
  return Promise.reject(error);
});

// Helper per determinare se usare mock data quando il backend non risponde
export const handleApiErrorWithFallback = <T>(error: any, fallbackData: T): T => {
  if (error.code === 'ERR_NETWORK' || error.code === 'ECONNABORTED' || (error.response && error.response.status >= 500)) {
    console.warn('[AuraPay Frontend] Backend non raggiungibile o errore di rete. Attivazione fallback dati simulati (Mock Data Mode).', error);
    return fallbackData;
  }
  throw error;
};
