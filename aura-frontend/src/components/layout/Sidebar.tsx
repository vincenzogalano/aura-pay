import React from 'react';
import { NavLink } from 'react-router-dom';
import { 
  LayoutDashboard, 
  CreditCard, 
  UserCheck, 
  Key, 
  FileText, 
  Webhook, 
  Zap,
  ExternalLink
} from 'lucide-react';
import { useMerchant } from '../../context/MerchantContext';

export const Sidebar: React.FC = () => {
  const { merchant } = useMerchant();

  const navItems = [
    { label: 'Overview', path: '/dashboard', icon: LayoutDashboard },
    { label: 'Transazioni', path: '/transactions', icon: CreditCard },
    { label: 'KYB & Profilo', path: '/onboarding', icon: UserCheck, badge: merchant.status === 'VERIFIED' ? 'LIVE OK' : 'PENDING' },
    { label: 'API Keys', path: '/api-keys', icon: Key },
    { label: 'Fatture PDF', path: '/invoices', icon: FileText },
    { label: 'Webhooks', path: '/webhooks', icon: Webhook },
  ];

  return (
    <aside className="w-64 bg-slate-950 border-r border-slate-800/80 flex flex-col justify-between h-screen sticky top-0 z-40">
      <div>
        {/* Brand Logo Header */}
        <div className="p-6 flex items-center gap-3 border-b border-slate-800/60">
          <div className="w-10 h-10 rounded-xl bg-indigo-600 flex items-center justify-center text-white shadow-glow-indigo">
            <Zap className="w-6 h-6 fill-current text-white" />
          </div>
          <div>
            <h1 className="font-extrabold text-lg text-white tracking-tight">AuraPay</h1>
            <p className="text-[10px] font-medium text-indigo-400 uppercase tracking-wider">Merchant Portal</p>
          </div>
        </div>

        {/* Navigation Menu */}
        <nav className="px-3 py-6 space-y-1.5">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }: { isActive: boolean }) =>
                  `flex items-center justify-between px-3.5 py-3 rounded-xl text-sm font-medium transition-all duration-200 ${
                    isActive
                      ? 'bg-indigo-600/15 text-indigo-400 border border-indigo-500/30 shadow-glow-indigo font-semibold'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
                  }`
                }
              >
                <div className="flex items-center gap-3">
                  <Icon className="w-5 h-5" />
                  <span>{item.label}</span>
                </div>
                {item.badge && (
                  <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase ${
                    item.badge === 'LIVE OK' 
                      ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30' 
                      : 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                  }`}>
                    {item.badge}
                  </span>
                )}
              </NavLink>
            );
          })}
        </nav>
      </div>

      {/* Footer Info & Documentation */}
      <div className="p-4 border-t border-slate-800/60">
        <div className="glass-card p-3 text-xs space-y-2">
          <div className="font-semibold text-slate-300 flex items-center justify-between">
            <span>AuraPay API v1.0</span>
            <ExternalLink className="w-3.5 h-3.5 text-slate-500" />
          </div>
          <p className="text-slate-500 text-[11px]">
            Event-Driven Payment Engine con ledger a partita doppia.
          </p>
        </div>
      </div>
    </aside>
  );
};
