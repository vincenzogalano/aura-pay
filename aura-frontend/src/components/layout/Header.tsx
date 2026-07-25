import React from 'react';
import { useMerchant } from '../../context/MerchantContext';
import { EnvironmentBadge } from '../common/EnvironmentBadge';
import { Activity, ChevronRight } from 'lucide-react';
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
    <header className="sticky top-0 z-30 bg-slate-900/80 backdrop-blur-xl border-b border-slate-800/80 px-6 py-4 flex items-center justify-between transition-all">
      {/* Left side: Breadcrumb / Active Environment */}
      <div className="flex items-center gap-4">
        <EnvironmentBadge isTest={isTest} />
        
        <div className="hidden sm:flex items-center gap-2 text-xs text-slate-400">
          <span>AuraPay Engine</span>
          <ChevronRight className="w-3 h-3 text-slate-600" />
          <span className="text-slate-200 font-medium">{merchant.businessName}</span>
        </div>
      </div>

      {/* Right side: Environment Switcher, Platform Status, Merchant Profile */}
      <div className="flex items-center gap-5">
        {/* Toggle Switch */}
        <div className="flex items-center gap-2 bg-slate-950/70 border border-slate-800 p-1.5 rounded-xl">
          <button
            onClick={handleToggle}
            className={`flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-lg transition-all ${
              isTest 
                ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30 shadow-sm' 
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Sandbox
          </button>
          <button
            onClick={handleToggle}
            className={`flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-lg transition-all ${
              !isTest 
                ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 shadow-sm' 
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Live
          </button>
        </div>

        {/* Platform Status Indicator */}
        <div className="hidden md:flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-950/50 border border-slate-800/60 text-xs text-slate-300">
          <Activity className="w-3.5 h-3.5 text-emerald-400 animate-pulse" />
          <span>System Operational</span>
        </div>

        {/* Merchant Avatar */}
        <div className="flex items-center gap-3 pl-3 border-l border-slate-800">
          <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-indigo-600 to-indigo-400 flex items-center justify-center text-white font-bold text-sm shadow-glow-indigo">
            {merchant.businessName.substring(0, 2).toUpperCase()}
          </div>
          <div className="hidden lg:block text-left">
            <div className="text-xs font-semibold text-slate-200">{merchant.businessName}</div>
            <div className="text-[10px] text-slate-400">{merchant.email}</div>
          </div>
        </div>
      </div>
    </header>
  );
};
