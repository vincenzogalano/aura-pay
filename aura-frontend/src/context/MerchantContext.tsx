import React, { createContext, useContext, useState, useEffect } from 'react';
import type { Merchant, ApiKey } from '../types';
import { mockMerchant, mockApiKeys } from '../api/mockData';

interface MerchantContextType {
  merchant: Merchant;
  isTest: boolean;
  activeApiKey: string;
  apiKeys: ApiKey[];
  toggleEnvironment: (mode?: boolean) => void;
  updateMerchant: (updated: Partial<Merchant>) => void;
  setMerchantProfile: (merchant: Merchant) => void;
  addApiKey: (key: ApiKey) => void;
  revokeApiKey: (keyId: string) => void;
}

const MerchantContext = createContext<MerchantContextType | undefined>(undefined);

export const MerchantProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [merchant, setMerchant] = useState<Merchant>(() => {
    const saved = localStorage.getItem('aurapay_merchant');
    return saved ? JSON.parse(saved) : mockMerchant;
  });

  const [isTest, setIsTest] = useState<boolean>(() => {
    const savedMode = localStorage.getItem('aurapay_is_test');
    return savedMode !== null ? JSON.parse(savedMode) : true;
  });

  const [apiKeys, setApiKeys] = useState<ApiKey[]>(() => {
    const savedKeys = localStorage.getItem('aurapay_api_keys');
    return savedKeys ? JSON.parse(savedKeys) : mockApiKeys;
  });

  // Trova la prima API Key attiva per l'ambiente corrente
  const activeKeyObj = apiKeys.find(k => k.environment === (isTest ? 'TEST' : 'LIVE') && !k.revokedAt);
  const activeApiKey = activeKeyObj?.keySecret || activeKeyObj?.keyPrefix || (isTest ? 'sk_test_mock' : 'sk_live_mock');

  useEffect(() => {
    localStorage.setItem('aurapay_merchant', JSON.stringify(merchant));
  }, [merchant]);

  useEffect(() => {
    localStorage.setItem('aurapay_is_test', JSON.stringify(isTest));
    localStorage.setItem('aurapay_active_api_key', activeApiKey);
  }, [isTest, activeApiKey]);

  useEffect(() => {
    localStorage.setItem('aurapay_api_keys', JSON.stringify(apiKeys));
  }, [apiKeys]);

  const toggleEnvironment = (mode?: boolean) => {
    const next = mode !== undefined ? mode : !isTest;
    setIsTest(next);
  };

  const updateMerchant = (updated: Partial<Merchant>) => {
    setMerchant(prev => ({ ...prev, ...updated }));
  };

  const setMerchantProfile = (newMerchant: Merchant) => {
    setMerchant(newMerchant);
  };

  const addApiKey = (key: ApiKey) => {
    setApiKeys(prev => [key, ...prev]);
  };

  const revokeApiKey = (keyId: string) => {
    setApiKeys(prev => prev.map(k => k.id === keyId ? { ...k, revokedAt: new Date().toISOString() } : k));
  };

  return (
    <MerchantContext.Provider
      value={{
        merchant,
        isTest,
        activeApiKey,
        apiKeys,
        toggleEnvironment,
        updateMerchant,
        setMerchantProfile,
        addApiKey,
        revokeApiKey,
      }}
    >
      {children}
    </MerchantContext.Provider>
  );
};

export const useMerchant = (): MerchantContextType => {
  const context = useContext(MerchantContext);
  if (!context) {
    throw new Error('useMerchant deve essere utilizzato all\'interno di MerchantProvider');
  }
  return context;
};
