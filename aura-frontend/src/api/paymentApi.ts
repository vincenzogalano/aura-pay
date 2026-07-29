import { apiClient } from './client';
import type { PaymentIntent, PaymentStatus } from '../types';

function ensureUuid(id?: string): string {
  if (!id) return 'c7c24292-2cc6-360e-8bf0-40ce22304a40';
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  if (uuidRegex.test(id)) return id;
  return 'c7c24292-2cc6-360e-8bf0-40ce22304a40';
}

export const paymentApi = {
  // GET /v1/payments
  getPayments: async (params?: { isTest?: boolean; status?: PaymentStatus; limit?: number }): Promise<PaymentIntent[]> => {
    const response = await apiClient.get('/v1/payments', { params });
    const res = response.data;
    if (Array.isArray(res)) return res;
    if (res && Array.isArray(res.content)) return res.content;
    return [];
  },

  // GET /v1/payments/{id}
  getPaymentById: async (id: string): Promise<PaymentIntent> => {
    const response = await apiClient.get(`/v1/payments/${id}`);
    return response.data;
  },

  // POST /v1/payments
  createPaymentIntent: async (data: { merchantId?: string; amountCents: number; currency: string; description?: string; customerEmail?: string; isTest: boolean }): Promise<PaymentIntent> => {
    const payload = {
      ...data,
      merchantId: ensureUuid(data.merchantId),
    };
    const response = await apiClient.post('/v1/payments', payload);
    return response.data;
  },

  // POST /v1/payments/{id}/confirm
  confirmPayment: async (paymentId: string, data: { paymentMethodToken: string }): Promise<PaymentIntent> => {
    const response = await apiClient.post(`/v1/payments/${paymentId}/confirm`, data);
    return response.data;
  },

  // POST /v1/payments/{id}/refund
  refundPayment: async (paymentId: string, data: { amountCents: number; reason?: string }): Promise<PaymentIntent> => {
    const response = await apiClient.post(`/v1/payments/${paymentId}/refund`, data);
    return response.data;
  },
};
