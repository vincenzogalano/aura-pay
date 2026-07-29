import { apiClient } from './client';
import type { Invoice } from '../types';

export const invoiceApi = {
  // GET /v1/invoices?merchantId=...&isTest=...
  getInvoices: async (merchantId: string, isTest: boolean): Promise<Invoice[]> => {
    const response = await apiClient.get('/v1/invoices', {
      params: { merchantId, isTest }
    });
    const res = response.data;
    if (Array.isArray(res)) return res;
    if (res && Array.isArray(res.content)) return res.content;
    return [];
  },

  // GET /v1/invoices/{id}/download-url
  getDownloadUrl: async (invoiceId: string): Promise<{ downloadUrl: string; expiresAt: string }> => {
    const response = await apiClient.get(`/v1/invoices/${invoiceId}/download-url`);
    return response.data;
  },
};
