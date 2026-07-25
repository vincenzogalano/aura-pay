import { apiClient, handleApiErrorWithFallback } from './client';
import type { PaymentIntent, PaymentStatus } from '../types';
import { mockPayments } from './mockData';

export const paymentApi = {
  // GET /v1/payments
  getPayments: async (params?: { isTest?: boolean; status?: PaymentStatus; limit?: number }): Promise<PaymentIntent[]> => {
    try {
      const response = await apiClient.get('/v1/payments', { params });
      return response.data;
    } catch (error) {
      let filtered = mockPayments;
      if (params?.isTest !== undefined) {
        filtered = filtered.filter(p => p.isTest === params.isTest);
      }
      if (params?.status) {
        filtered = filtered.filter(p => p.status === params.status);
      }
      return handleApiErrorWithFallback(error, filtered);
    }
  },

  // GET /v1/payments/{id}
  getPaymentById: async (id: string): Promise<PaymentIntent> => {
    try {
      const response = await apiClient.get(`/v1/payments/${id}`);
      return response.data;
    } catch (error) {
      const found = mockPayments.find(p => p.id === id) || mockPayments[0];
      return handleApiErrorWithFallback(error, found);
    }
  },

  // POST /v1/payments
  createPaymentIntent: async (data: { amountCents: number; currency: string; description?: string; customerEmail?: string; isTest: boolean }): Promise<PaymentIntent> => {
    try {
      const response = await apiClient.post('/v1/payments', data);
      return response.data;
    } catch (error) {
      return handleApiErrorWithFallback(error, {
        id: `pi_${Math.random().toString(36).substring(2, 9)}`,
        merchantId: 'mch_8f21ac99-4a2b-47e1-8899-123456789abc',
        amountCents: data.amountCents,
        currency: data.currency || 'EUR',
        status: 'CREATED',
        isTest: data.isTest,
        description: data.description,
        customerEmail: data.customerEmail,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      });
    }
  },

  // POST /v1/payments/{id}/refund
  refundPayment: async (paymentId: string, data: { amountCents: number; reason?: string }): Promise<PaymentIntent> => {
    try {
      const response = await apiClient.post(`/v1/payments/${paymentId}/refund`, data);
      return response.data;
    } catch (error) {
      const existing = mockPayments.find(p => p.id === paymentId) || mockPayments[0];
      const newRefundedTotal = (existing.refundedAmountCents || 0) + data.amountCents;
      const isTotalRefund = newRefundedTotal >= existing.amountCents;

      return handleApiErrorWithFallback(error, {
        ...existing,
        status: isTotalRefund ? 'REFUNDED' : 'PARTIALLY_REFUNDED',
        refundedAmountCents: newRefundedTotal,
        updatedAt: new Date().toISOString(),
      });
    }
  },
};
