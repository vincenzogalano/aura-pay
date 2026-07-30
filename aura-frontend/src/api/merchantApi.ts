import { apiClient } from './client';
import type { Merchant, ApiKey } from '../types';

export const merchantApi = {
  // GET /v1/merchants
  getAllMerchants: async (): Promise<Merchant[]> => {
    const response = await apiClient.get('/v1/merchants');
    const data = response.data;
    if (Array.isArray(data)) return data;
    if (data && Array.isArray(data.content)) return data.content;
    return [];
  },

  // POST /v1/merchants/register
  registerMerchant: async (data: { businessName: string; vatNumber: string; email: string; country: string }): Promise<{ merchant: Merchant; apiKeys: ApiKey[] }> => {
    const response = await apiClient.post('/v1/merchants/register', data);
    return response.data;
  },

  // GET /v1/merchants/{id}
  getMerchantProfile: async (merchantId: string): Promise<Merchant> => {
    const response = await apiClient.get(`/v1/merchants/${merchantId}`);
    return response.data;
  },

  // PUT /v1/merchants/{id}
  updateMerchantProfile: async (merchantId: string, data: { businessName: string; email: string }): Promise<Merchant> => {
    const response = await apiClient.put(`/v1/merchants/${merchantId}`, data);
    return response.data;
  },

  // POST /v1/merchants/{id}/verification-request
  requestKYBVerification: async (merchantId: string, payload: { registrationNumber: string; businessAddress: string; legalRepresentative: string }): Promise<Merchant> => {
    const response = await apiClient.post(`/v1/merchants/${merchantId}/verification-request`, payload);
    return response.data;
  },

  // GET /v1/merchants/{id}/api-keys
  getApiKeys: async (merchantId: string): Promise<ApiKey[]> => {
    const response = await apiClient.get(`/v1/merchants/${merchantId}/api-keys`);
    const data = response.data;
    if (Array.isArray(data)) return data;
    if (data && Array.isArray(data.content)) return data.content;
    return [];
  },

  // POST /v1/merchants/{id}/api-keys/live
  generateLiveKeys: async (merchantId: string): Promise<ApiKey[]> => {
    const response = await apiClient.post(`/v1/merchants/${merchantId}/api-keys/live`);
    const data = response.data;
    if (Array.isArray(data)) return data;
    if (data && typeof data === 'object') return [data];
    return [];
  },

  // POST /v1/merchants/{id}/api-keys/test
  generateTestKeys: async (merchantId: string): Promise<ApiKey[]> => {
    const response = await apiClient.post(`/v1/merchants/${merchantId}/api-keys/test`);
    const data = response.data;
    if (Array.isArray(data)) return data;
    if (data && typeof data === 'object') return [data];
    return [];
  },

  // POST /v1/merchants/{id}/api-keys/{keyId}/revoke
  revokeApiKey: async (merchantId: string, keyId: string): Promise<ApiKey> => {
    const response = await apiClient.post(`/v1/merchants/${merchantId}/api-keys/${keyId}/revoke`);
    return response.data;
  },
};
