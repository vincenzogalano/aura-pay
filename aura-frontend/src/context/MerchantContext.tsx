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
  allMerchants: Merchant[];
  isTest: boolean;
  activeApiKey: string;
  apiKeys: ApiKey[];
  toggleEnvironment: (mode?: boolean) => void;
  updateMerchant: (updated: Partial<Merchant>) => void;
  setMerchantProfile: (merchant: Merchant) => void;
  selectMerchant: (merchantId: string) => void;
  refreshMerchants: () => Promise<void>;
  addApiKey: (key: ApiKey) => void;
  revokeApiKey: (keyId: string) => void;
}

const MerchantContext = createContext<MerchantContextType | undefined>(undefined);

export const MerchantProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [merchant, setMerchant] = useState<Merchant>(defaultMerchant);
  const [allMerchants, setAllMerchants] = useState<Merchant[]>([defaultMerchant]);

  const [isTest, setIsTest] = useState<boolean>(() => {
    const savedMode = localStorage.getItem('aurapay_is_test');
    return savedMode !== null ? JSON.parse(savedMode) : true;
  });

  const [apiKeys, setApiKeys] = useState<ApiKey[]>(defaultKeys);

  const loadMerchantData = async (mId: string) => {
    try {
      const data = await merchantApi.getMerchantProfile(mId);
      setMerchant(data);
    } catch {
      // Fallback
    }

    try {
      const keys = await merchantApi.getApiKeys(mId);
      setApiKeys(keys.length > 0 ? keys : defaultKeys);
    } catch {
      setApiKeys(defaultKeys);
    }
  };

  const refreshMerchants = async () => {
    try {
      const list = await merchantApi.getAllMerchants();
      if (list.length > 0) {
        setAllMerchants(list);
      }
    } catch (err) {
      console.error('Errore nel recupero dell\'elenco merchant:', err);
    }
  };

  // Carica l'elenco dei merchant e il merchant selezionato all'avvio
  useEffect(() => {
    const savedId = localStorage.getItem('aurapay_selected_merchant_id') || defaultMerchant.id;
    refreshMerchants();
    loadMerchantData(savedId);
  }, []);

  const selectMerchant = (mId: string) => {
    localStorage.setItem('aurapay_selected_merchant_id', mId);
    const found = allMerchants.find(m => m.id === mId);
    if (found) {
      setMerchant(found);
    }
    loadMerchantData(mId);
  };

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
    refreshMerchants();
  };

  const setMerchantProfile = (newMerchant: Merchant) => {
    setMerchant(newMerchant);
    localStorage.setItem('aurapay_selected_merchant_id', newMerchant.id);
    refreshMerchants();
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
        allMerchants,
        isTest,
        activeApiKey,
        apiKeys,
        toggleEnvironment,
        updateMerchant,
        setMerchantProfile,
        selectMerchant,
        refreshMerchants,
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
