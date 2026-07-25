import { apiClient, handleApiErrorWithFallback } from './client';
import type { Merchant, ApiKey } from '../types';
import { mockMerchant, mockApiKeys } from './mockData';

export const merchantApi = {
  // POST /v1/merchants/register
  registerMerchant: async (data: { businessName: string; vatNumber: string; email: string; country: string }): Promise<{ merchant: Merchant; apiKeys: ApiKey[] }> => {
    try {
      const response = await apiClient.post('/v1/merchants/register', data);
      return response.data;
    } catch (error) {
      return handleApiErrorWithFallback(error, {
        merchant: {
          ...mockMerchant,
          businessName: data.businessName,
          vatNumber: data.vatNumber,
          email: data.email,
          country: data.country || 'IT',
          status: 'PENDING_VERIFICATION',
        },
        apiKeys: mockApiKeys.filter(k => k.environment === 'TEST'),
      });
    }
  },

  // GET /v1/merchants/{id}
  getMerchantProfile: async (merchantId: string): Promise<Merchant> => {
    try {
      const response = await apiClient.get(`/v1/merchants/${merchantId}`);
      return response.data;
    } catch (error) {
      return handleApiErrorWithFallback(error, mockMerchant);
    }
  },

  // POST /v1/merchants/{id}/verification-request
  requestKYBVerification: async (merchantId: string, payload: { taxId: string; address: string; legalRepresentative: string }): Promise<Merchant> => {
    try {
      const response = await apiClient.post(`/v1/merchants/${merchantId}/verification-request`, payload);
      return response.data;
    } catch (error) {
      return handleApiErrorWithFallback(error, {
        ...mockMerchant,
        status: 'VERIFIED', // Simula approvazione KYB immediata nel mock
      });
    }
  },

  // GET /v1/merchants/{id}/api-keys
  getApiKeys: async (merchantId: string): Promise<ApiKey[]> => {
    try {
      const response = await apiClient.get(`/v1/merchants/${merchantId}/api-keys`);
      return response.data;
    } catch (error) {
      return handleApiErrorWithFallback(error, mockApiKeys);
    }
  },

  // POST /v1/merchants/{id}/api-keys/live
  generateLiveKeys: async (merchantId: string): Promise<ApiKey> => {
    try {
      const response = await apiClient.post(`/v1/merchants/${merchantId}/api-keys/live`);
      return response.data;
    } catch (error) {
      return handleApiErrorWithFallback(error, {
        id: `key_live_${Date.now()}`,
        merchantId,
        keyPrefix: 'sk_live_new99',
        environment: 'LIVE',
        revokedAt: null,
        createdAt: new Date().toISOString(),
        keySecret: 'sk_live_demo_mock_key_aura_generated',
      });
    }
  },
};
