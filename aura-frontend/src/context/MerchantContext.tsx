import React, { createContext, useContext, useState, useEffect } from 'react';
import type { Merchant, ApiKey } from '../types';
import { merchantApi } from '../api/merchantApi';

const defaultMerchant: Merchant = {
  id: 'mch_acme_tech_2026',
  businessName: 'Acme Tech Solutions S.r.l.',
  vatNumber: 'IT12345678901',
  email: 'amministrazione@acmetech.it',
  status: 'VERIFIED',
  country: 'Italia',
  createdAt: new Date().toISOString(),
};

const defaultKeys: ApiKey[] = [
  {
    id: 'key_test_01',
    merchantId: 'mch_acme_tech_2026',
    keyPrefix: 'sk_test_a1b2',
    environment: 'TEST',
    revokedAt: null,
    createdAt: new Date().toISOString(),
    keySecret: 'sk_test_demo_key_aurapay_2026_sandbox',
  },
  {
    id: 'key_live_01',
    merchantId: 'mch_acme_tech_2026',
    keyPrefix: 'sk_live_99zz',
    environment: 'LIVE',
    revokedAt: null,
    createdAt: new Date().toISOString(),
    keySecret: 'sk_live_demo_key_aurapay_2026_production',
  },
];

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
  const [merchant, setMerchant] = useState<Merchant>(defaultMerchant);

  const [isTest, setIsTest] = useState<boolean>(() => {
    const savedMode = localStorage.getItem('aurapay_is_test');
    return savedMode !== null ? JSON.parse(savedMode) : true;
  });

  const [apiKeys, setApiKeys] = useState<ApiKey[]>(defaultKeys);

  // Carica i dati reali del merchant dall'API Backend
  useEffect(() => {
    merchantApi.getMerchantProfile('mch_acme_tech_2026')
      .then((data) => setMerchant(data))
      .catch(() => {});

    merchantApi.getApiKeys('mch_acme_tech_2026')
      .then((keys) => setApiKeys(keys))
      .catch(() => {});
  }, []);

  const activeKeyObj = apiKeys.find(k => k.environment === (isTest ? 'TEST' : 'LIVE') && !k.revokedAt);
  const activeApiKey = activeKeyObj?.keySecret || activeKeyObj?.keyPrefix || (isTest ? 'sk_test_a1b2' : 'sk_live_99zz');

  useEffect(() => {
    localStorage.setItem('aurapay_is_test', JSON.stringify(isTest));
    localStorage.setItem('aurapay_active_api_key', activeApiKey);
  }, [isTest, activeApiKey]);

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

  const revokeApiKey = async (keyId: string) => {
    setApiKeys(prev => prev.map(k => k.id === keyId ? { ...k, revokedAt: new Date().toISOString() } : k));
    try {
      await merchantApi.revokeApiKey(merchant.id, keyId);
    } catch (err) {
      console.error("Errore durante la revoca della chiave API nel backend:", err);
    }
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
