import { apiClient } from './client';

export interface TokenizeRequestData {
  cardNumber: string;
  cardholderName: string;
  expirationMonth: number;
  expirationYear: number;
  cvv: string;
}

export interface TokenResponseData {
  token: string;
  maskedPan: string;
  cardBrand: string;
  cardholderName: string;
  expirationMonth: number;
  expirationYear: number;
  createdAt: string;
  expiresAt: string;
  livemode: boolean;
}

export const vaultApi = {
  // POST /v1/tokens
  tokenize: async (data: TokenizeRequestData): Promise<TokenResponseData> => {
    const response = await apiClient.post('/v1/tokens', data);
    return response.data;
  },
};
