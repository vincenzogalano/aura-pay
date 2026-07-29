import { apiClient } from './client';
import type { WebhookSubscription, WebhookDelivery } from '../types';

export const webhookApi = {
  // GET /v1/webhooks/subscriptions?merchantId=...&isTest=...
  getSubscriptions: async (merchantId: string, isTest: boolean): Promise<WebhookSubscription[]> => {
    const response = await apiClient.get('/v1/webhooks/subscriptions', {
      params: { merchantId, isTest }
    });
    const res = response.data;
    if (Array.isArray(res)) return res;
    if (res && Array.isArray(res.content)) return res.content;
    if (res && typeof res === 'object' && res.id) return [res];
    return [];
  },

  // POST /v1/webhooks/subscriptions
  createSubscription: async (data: { merchantId: string; targetUrl: string; events: string[]; isTest: boolean }): Promise<WebhookSubscription> => {
    const response = await apiClient.post('/v1/webhooks/subscriptions', data);
    return response.data;
  },

  // DELETE /v1/webhooks/subscriptions/{id}
  deleteSubscription: async (id: string): Promise<void> => {
    await apiClient.delete(`/v1/webhooks/subscriptions/${id}`);
  },

  // GET /v1/webhooks/deliveries?merchantId=...
  getDeliveries: async (merchantId?: string): Promise<WebhookDelivery[]> => {
    const response = await apiClient.get('/v1/webhooks/deliveries', {
      params: { merchantId }
    });
    const res = response.data;
    if (Array.isArray(res)) return res;
    if (res && Array.isArray(res.content)) return res.content;
    return [];
  },

  // POST /v1/webhooks/deliveries/{id}/replay
  replayDelivery: async (deliveryId: string): Promise<WebhookDelivery> => {
    const response = await apiClient.post(`/v1/webhooks/deliveries/${deliveryId}/replay`);
    return response.data;
  },
};
