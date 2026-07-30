import React from 'react';
import { useMerchant } from '../../context/MerchantContext';
import { EnvironmentBadge } from '../common/EnvironmentBadge';
import { toast } from 'sonner';

export const Header: React.FC = () => {
  const { merchant, allMerchants, selectMerchant, isTest, toggleEnvironment } = useMerchant();

  const handleToggle = () => {
    if (merchant.status !== 'VERIFIED' && isTest) {
      toast.error('Modalità LIVE disabilitata! Devi completare la verifica KYB per accedere al LIVE.');
      return;
    }
    const nextMode = !isTest;
    toggleEnvironment(nextMode);
    toast.success(`Ambiente commutato in modalità ${nextMode ? 'SANDBOX (TEST)' : 'LIVE'}`);
  };

  return (
    <header className="sticky top-0 z-30 bg-white border-b border-zinc-200 px-6 py-3.5 flex items-center justify-between">
      {/* Left side: Environment Badge & Merchant Selector */}
      <div className="flex items-center gap-4">
        <EnvironmentBadge isTest={isTest} />
        
        <div className="flex items-center gap-2 text-xs text-zinc-500">
          <span className="hidden sm:inline">Esercente:</span>
          <select
            value={merchant.id}
            onChange={(e) => {
              selectMerchant(e.target.value);
              toast.info(`Esercente selezionato: ${allMerchants.find(m => m.id === e.target.value)?.businessName || e.target.value}`);
            }}
            className="shadcn-input text-xs font-semibold cursor-pointer bg-zinc-50 border-zinc-300 py-1 px-2.5 rounded text-zinc-900 focus:ring-1 focus:ring-zinc-400"
          >
            {(Array.isArray(allMerchants) ? allMerchants : []).map((m, idx) => (
              <option key={m.id ? `mch-${m.id}` : `mch-idx-${idx}`} value={m.id}>
                {m.businessName || 'Esercente Senza Nome'} ({m.status === 'VERIFIED' ? 'Verificato' : 'In Attesa'})
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Right side: Environment Switcher & Merchant Profile */}
      <div className="flex items-center gap-4">
        {/* Toggle Switch */}
        <div className="flex items-center gap-1 bg-zinc-100 border border-zinc-200 p-1 rounded-md">
          <button
            onClick={handleToggle}
            className={`text-xs font-medium px-3 py-1 rounded transition-colors ${
              isTest 
                ? 'bg-amber-100 text-amber-900 border border-amber-300 font-semibold' 
                : 'text-zinc-500 hover:text-zinc-900'
            }`}
          >
            Ambiente Prova (Sandbox)
          </button>
          <button
            onClick={handleToggle}
            className={`text-xs font-medium px-3 py-1 rounded transition-colors ${
              !isTest 
                ? 'bg-emerald-100 text-emerald-900 border border-emerald-300 font-semibold' 
                : 'text-zinc-500 hover:text-zinc-900'
            }`}
          >
            Ambiente Reale (Live)
          </button>
        </div>

        {/* Merchant Avatar */}
        <div className="flex items-center gap-2.5 pl-3 border-l border-zinc-200">
          <div className="w-7 h-7 rounded-full bg-zinc-900 text-white flex items-center justify-center font-bold text-xs">
            {(merchant.businessName || 'AP').substring(0, 2).toUpperCase()}
          </div>
          <div className="hidden lg:block text-left text-xs">
            <div className="font-semibold text-zinc-900">{merchant.businessName}</div>
          </div>
        </div>
      </div>
    </header>
  );
};
