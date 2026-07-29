import React from 'react';
import { useMerchant } from '../../context/MerchantContext';
import { EnvironmentBadge } from '../common/EnvironmentBadge';
import { ChevronRight } from 'lucide-react';
import { toast } from 'sonner';

export const Header: React.FC = () => {
  const { merchant, isTest, toggleEnvironment } = useMerchant();

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
      {/* Left side: Environment Badge & Merchant Name */}
      <div className="flex items-center gap-4">
        <EnvironmentBadge isTest={isTest} />
        
        <div className="hidden sm:flex items-center gap-2 text-xs text-zinc-500">
          <span>AuraPay Console</span>
          <ChevronRight className="w-3.5 h-3.5 text-zinc-400" />
          <span className="text-zinc-900 font-medium">{merchant.businessName}</span>
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
            Sandbox
          </button>
          <button
            onClick={handleToggle}
            className={`text-xs font-medium px-3 py-1 rounded transition-colors ${
              !isTest 
                ? 'bg-emerald-100 text-emerald-900 border border-emerald-300 font-semibold' 
                : 'text-zinc-500 hover:text-zinc-900'
            }`}
          >
            Live
          </button>
        </div>

        {/* Merchant Avatar */}
        <div className="flex items-center gap-2.5 pl-3 border-l border-zinc-200">
          <div className="w-7 h-7 rounded-full bg-zinc-900 text-white flex items-center justify-center font-bold text-xs">
            {merchant.businessName.substring(0, 2).toUpperCase()}
          </div>
          <div className="hidden lg:block text-left text-xs">
            <div className="font-semibold text-zinc-900">{merchant.businessName}</div>
          </div>
        </div>
      </div>
    </header>
  );
};
