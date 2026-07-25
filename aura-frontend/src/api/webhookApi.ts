import { apiClient, handleApiErrorWithFallback } from './client';
import type { WebhookSubscription, WebhookDelivery } from '../types';
import { mockWebhookSubscriptions, mockWebhookDeliveries } from './mockData';

export const webhookApi = {
  // GET /v1/webhooks/subscriptions?merchantId=...&isTest=...
  getSubscriptions: async (merchantId: string, isTest: boolean): Promise<WebhookSubscription[]> => {
    try {
      const response = await apiClient.get('/v1/webhooks/subscriptions', {
        params: { merchantId, isTest }
      });
      return response.data;
    } catch (error) {
      return handleApiErrorWithFallback(error, mockWebhookSubscriptions.filter(s => s.isTest === isTest));
    }
  },

  // POST /v1/webhooks/subscriptions
  createSubscription: async (data: { merchantId: string; targetUrl: string; events: string[]; isTest: boolean }): Promise<WebhookSubscription> => {
    try {
      const response = await apiClient.post('/v1/webhooks/subscriptions', data);
      return response.data;
    } catch (error) {
      return handleApiErrorWithFallback(error, {
        id: `sub_${Math.random().toString(36).substring(2, 8)}`,
        merchantId: data.merchantId,
        targetUrl: data.targetUrl,
        events: data.events,
        secretKey: `whsec_${Math.random().toString(36).substring(2, 16)}`,
        isTest: data.isTest,
        createdAt: new Date().toISOString(),
      });
    }
  },

  // GET /v1/webhooks/deliveries?subscriptionId=...
  getDeliveries: async (subscriptionId?: string): Promise<WebhookDelivery[]> => {
    try {
      const response = await apiClient.get('/v1/webhooks/deliveries', {
        params: { subscriptionId }
      });
      return response.data;
    } catch (error) {
      return handleApiErrorWithFallback(error, mockWebhookDeliveries);
    }
  },

  // POST /v1/webhooks/deliveries/{id}/replay
  replayDelivery: async (deliveryId: string): Promise<WebhookDelivery> => {
    try {
      const response = await apiClient.post(`/v1/webhooks/deliveries/${deliveryId}/replay`);
      return response.data;
    } catch (error) {
      const existing = mockWebhookDeliveries.find(d => d.id === deliveryId) || mockWebhookDeliveries[0];
      return handleApiErrorWithFallback(error, {
        ...existing,
        attemptCount: existing.attemptCount + 1,
        status: 'SUCCESS',
        httpStatus: 200,
        lastError: null,
      });
    }
  },
};
