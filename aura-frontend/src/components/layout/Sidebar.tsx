import React from 'react';
import { NavLink } from 'react-router-dom';
import { 
  LayoutDashboard, 
  CreditCard, 
  UserCheck, 
  Key, 
  FileText, 
  Webhook, 
  PlayCircle,
  Terminal,
  ShieldCheck,
  Radio,
  Scale,
  Network
} from 'lucide-react';
import { useMerchant } from '../../context/MerchantContext';

export const Sidebar: React.FC = () => {
  const { merchant } = useMerchant();

  const mainItems = [
    { label: 'Panoramica Generale', path: '/dashboard', icon: LayoutDashboard },
    { label: 'Transazioni Incassi', path: '/transactions', icon: CreditCard },
    { label: 'Mastro Contabile', path: '/ledger', icon: Scale },
    { label: 'Verifica Societaria KYB', path: '/onboarding', icon: UserCheck, badge: merchant.status === 'VERIFIED' ? 'ABILITATO' : 'IN ATTESA' },
    { label: 'Chiavi API', path: '/api-keys', icon: Key },
    { label: 'Fatture e Note di Credito', path: '/invoices', icon: FileText },
    { label: 'Notifiche Webhook', path: '/webhooks', icon: Webhook },
  ];

  const devToolsItems = [
    { label: 'Simulatore Checkout', path: '/checkout-demo', icon: PlayCircle },
    { label: 'Console Sviluppatori', path: '/developer', icon: Terminal },
    { label: 'Flusso Eventi Kafka', path: '/event-stream', icon: Radio },
    { label: 'Mappa Architettura', path: '/architecture', icon: Network },
  ];

  return (
    <aside className="w-64 bg-white border-r border-zinc-200 flex flex-col justify-between h-screen sticky top-0 z-40">
      <div>
        {/* Brand Header */}
        <div className="p-5 flex items-center justify-between border-b border-zinc-200">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-md bg-zinc-900 flex items-center justify-center text-white font-bold text-sm">
              A
            </div>
            <div>
              <h1 className="font-bold text-sm text-zinc-900 tracking-tight">AuraPay</h1>
              <p className="text-[10px] text-zinc-500 font-medium">Pannello Gestione Esercente</p>
            </div>
          </div>
          <ShieldCheck className="w-4 h-4 text-emerald-600" />
        </div>

        {/* Navigation Menu */}
        <nav className="px-3 py-4 space-y-6 text-xs">
          <div className="space-y-1">
            <div className="px-3 pb-1.5 text-[10px] font-semibold text-zinc-400 uppercase tracking-wider">Gestione Operativa</div>
            {mainItems.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  className={({ isActive }: { isActive: boolean }) =>
                    `flex items-center justify-between px-3 py-2 rounded-md font-medium transition-colors ${
                      isActive
                        ? 'bg-zinc-100 text-zinc-900 font-semibold'
                        : 'text-zinc-600 hover:text-zinc-900 hover:bg-zinc-50'
                    }`
                  }
                >
                  <div className="flex items-center gap-2.5">
                    <Icon className="w-4 h-4" />
                    <span>{item.label}</span>
                  </div>
                  {item.badge && (
                    <span className={`text-[9px] px-1.5 py-0.5 rounded font-semibold ${
                      item.badge === 'ABILITATO' 
                        ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' 
                        : 'bg-amber-50 text-amber-700 border border-amber-200'
                    }`}>
                      {item.badge}
                    </span>
                  )}
                </NavLink>
              );
            })}
          </div>

          <div className="space-y-1 pt-3 border-t border-zinc-200">
            <div className="px-3 pb-1.5 text-[10px] font-semibold text-zinc-400 uppercase tracking-wider">Strumenti di Sviluppo</div>
            {devToolsItems.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  className={({ isActive }: { isActive: boolean }) =>
                    `flex items-center justify-between px-3 py-2 rounded-md font-medium transition-colors ${
                      isActive
                        ? 'bg-zinc-100 text-zinc-900 font-semibold'
                        : 'text-zinc-600 hover:text-zinc-900 hover:bg-zinc-50'
                    }`
                  }
                >
                  <div className="flex items-center gap-2.5">
                    <Icon className="w-4 h-4" />
                    <span>{item.label}</span>
                  </div>
                </NavLink>
              );
            })}
          </div>
        </nav>
      </div>

      {/* Footer Info */}
      <div className="p-4 border-t border-zinc-200">
        <div className="p-3 rounded-lg bg-zinc-50 border border-zinc-200 text-xs space-y-1">
          <div className="font-semibold text-zinc-800">AuraPay Gateway v1.0</div>
          <p className="text-zinc-500 text-[11px] leading-relaxed">
            Infrastruttura ad eventi con Ledger in partita doppia.
          </p>
        </div>
      </div>
    </aside>
  );
};
