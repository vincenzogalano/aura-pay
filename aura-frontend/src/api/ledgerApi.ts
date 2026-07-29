import { apiClient } from './client';
import type { LedgerBalance, LedgerEntry } from '../types';

export const ledgerApi = {
  // GET /v1/ledger/balance?merchantId=...&isTest=...
  getMerchantBalance: async (merchantId: string, isTest: boolean): Promise<LedgerBalance> => {
    const response = await apiClient.get('/v1/ledger/balance', {
      params: { merchantId, isTest }
    });
    return response.data;
  },

  // GET /v1/ledger/entries?merchantId=...&isTest=...
  getLedgerEntries: async (merchantId: string, isTest: boolean): Promise<LedgerEntry[]> => {
    const response = await apiClient.get('/v1/ledger/entries', {
      params: { merchantId, isTest }
    });
    const res = response.data;
    if (Array.isArray(res)) return res;
    if (res && Array.isArray(res.content)) return res.content;
    return [];
  },
};
