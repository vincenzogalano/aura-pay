import { apiClient, handleApiErrorWithFallback } from './client';
import type { LedgerBalance, LedgerEntry } from '../types';
import { mockLedgerBalances, mockLedgerEntries } from './mockData';

export const ledgerApi = {
  // GET /v1/ledger/balance?merchantId=...&isTest=...
  getMerchantBalance: async (merchantId: string, isTest: boolean): Promise<LedgerBalance> => {
    try {
      const response = await apiClient.get('/v1/ledger/balance', {
        params: { merchantId, isTest }
      });
      return response.data;
    } catch (error) {
      const mode = isTest ? 'TEST' : 'LIVE';
      return handleApiErrorWithFallback(error, mockLedgerBalances[mode]);
    }
  },

  // GET /v1/ledger/entries?merchantId=...&isTest=...
  getLedgerEntries: async (merchantId: string, isTest: boolean): Promise<LedgerEntry[]> => {
    try {
      const response = await apiClient.get('/v1/ledger/entries', {
        params: { merchantId, isTest }
      });
      return response.data;
    } catch (error) {
      return handleApiErrorWithFallback(error, mockLedgerEntries.filter(e => e.isTest === isTest));
    }
  },
};
