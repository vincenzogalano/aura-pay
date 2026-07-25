import { apiClient, handleApiErrorWithFallback } from './client';
import type { Invoice } from '../types';
import { mockInvoices } from './mockData';

export const invoiceApi = {
  // GET /v1/invoices?merchantId=...&isTest=...
  getInvoices: async (merchantId: string, isTest: boolean): Promise<Invoice[]> => {
    try {
      const response = await apiClient.get('/v1/invoices', {
        params: { merchantId, isTest }
      });
      return response.data;
    } catch (error) {
      return handleApiErrorWithFallback(error, mockInvoices.filter(i => i.isTest === isTest));
    }
  },

  // GET /v1/invoices/{id}/download-url
  getDownloadUrl: async (invoiceId: string): Promise<{ downloadUrl: string; expiresAt: string }> => {
    try {
      const response = await apiClient.get(`/v1/invoices/${invoiceId}/download-url`);
      return response.data;
    } catch (error) {
      // In mock mode, produciamo un presigned URL o blob fittizio
      return handleApiErrorWithFallback(error, {
        downloadUrl: `http://localhost:9000/aurapay-invoices/${invoiceId}.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Expires=900&X-Amz-Signature=mock_signature_123456`,
        expiresAt: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
      });
    }
  },
};
