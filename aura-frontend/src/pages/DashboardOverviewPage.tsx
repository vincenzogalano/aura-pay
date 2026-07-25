import React, { useEffect, useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { ledgerApi } from '../api/ledgerApi';
import { paymentApi } from '../api/paymentApi';
import type { PaymentIntent, LedgerBalance } from '../types';
import { 
  Wallet, 
  TrendingUp, 
  Percent, 
  CreditCard, 
  ArrowUpRight, 
  Clock,
  ShieldAlert
} from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

export const DashboardOverviewPage: React.FC = () => {
  const { merchant, isTest } = useMerchant();
  const [balance, setBalance] = useState<LedgerBalance | null>(null);
  const [payments, setPayments] = useState<PaymentIntent[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [balData, payData] = await Promise.all([
          ledgerApi.getMerchantBalance(merchant.id, isTest),
          paymentApi.getPayments({ isTest })
        ]);
        setBalance(balData);
        setPayments(payData);
      } catch (err) {
        console.error('Errore nel caricamento dati dashboard:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [merchant.id, isTest]);

  // Calcolo KPI
  const totalVolumeCents = payments
    .filter(p => p.status === 'SUCCEEDED' || p.status === 'PARTIALLY_REFUNDED')
    .reduce((acc, p) => acc + p.amountCents, 0);

  const totalFeesCents = Math.round(totalVolumeCents * 0.03); // 3% fee stimata

  // Dati per il grafico Recharts
  const chartData = [
    { day: 'Lun', volume: 450 },
    { day: 'Mar', volume: 820 },
    { day: 'Mer', volume: 610 },
    { day: 'Gio', volume: 1200 },
    { day: 'Ven', volume: totalVolumeCents / 100 },
    { day: 'Sab', volume: 950 },
    { day: 'Dom', volume: 1100 },
  ];

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Header Titolo */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
            Dashboard Overview
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Monitora saldi algebrici, volumi processati ed attività di pagamento per l'ambiente {isTest ? 'Sandbox' : 'Live'}.
          </p>
        </div>

        {isTest && (
          <div className="flex items-center gap-2 bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs px-4 py-2 rounded-xl">
            <ShieldAlert className="w-4 h-4 text-amber-400" />
            <span>Stai visualizzando dati simulati dell'ambiente **Sandbox (TEST)**</span>
          </div>
        )}
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* Saldo Disponibile (Ledger Service) */}
        <div className="glass-panel p-6 relative overflow-hidden group">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Saldo Disponibile</span>
            <div className="p-2.5 rounded-xl bg-indigo-600/20 border border-indigo-500/30 text-indigo-400">
              <Wallet className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-3xl font-extrabold text-white">
              €{balance ? (balance.availableBalanceCents / 100).toFixed(2) : '0.00'}
            </div>
            <div className="flex items-center gap-1.5 text-xs text-emerald-400 mt-2 font-medium">
              <ArrowUpRight className="w-4 h-4" />
              <span>Calcolato da Ledger (Partita Doppia)</span>
            </div>
          </div>
        </div>

        {/* Volume Processato */}
        <div className="glass-panel p-6 relative overflow-hidden group">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Volume Processato</span>
            <div className="p-2.5 rounded-xl bg-emerald-600/20 border border-emerald-500/30 text-emerald-400">
              <TrendingUp className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-3xl font-extrabold text-white">
              €{(totalVolumeCents / 100).toFixed(2)}
            </div>
            <div className="flex items-center gap-1.5 text-xs text-slate-400 mt-2">
              <span className="text-emerald-400 font-semibold">+14.2%</span> rispetto a settimana scorsa
            </div>
          </div>
        </div>

        {/* Commissioni Totali */}
        <div className="glass-panel p-6 relative overflow-hidden group">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Commissioni AuraPay</span>
            <div className="p-2.5 rounded-xl bg-amber-600/20 border border-amber-500/30 text-amber-400">
              <Percent className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-3xl font-extrabold text-white">
              €{(totalFeesCents / 100).toFixed(2)}
            </div>
            <div className="flex items-center gap-1.5 text-xs text-slate-400 mt-2">
              <span>Quota trattenuta a bilancio</span>
            </div>
          </div>
        </div>

        {/* Conteggio Transazioni */}
        <div className="glass-panel p-6 relative overflow-hidden group">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Transazioni Totali</span>
            <div className="p-2.5 rounded-xl bg-cyan-600/20 border border-cyan-500/30 text-cyan-400">
              <CreditCard className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-3xl font-extrabold text-white">
              {payments.length}
            </div>
            <div className="flex items-center gap-1.5 text-xs text-cyan-400 mt-2 font-medium">
              <span>{payments.filter(p => p.status === 'SUCCEEDED').length} approvate con successo</span>
            </div>
          </div>
        </div>
      </div>

      {/* Main Charts & Recent Transactions Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recharts Area Chart */}
        <div className="glass-panel p-6 lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-bold text-white">Volume Transazioni (EUR)</h2>
              <p className="text-xs text-slate-400">Andamento giornaliero dei pagamenti elaborati</p>
            </div>
            <span className="text-xs font-medium px-3 py-1 rounded-lg bg-slate-800 text-slate-300">Ultimi 7 Giorni</span>
          </div>

          <div className="h-64 w-full pt-4">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="colorVolume" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.4}/>
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <XAxis dataKey="day" stroke="#64748b" fontSize={12} tickLine={false} />
                <YAxis stroke="#64748b" fontSize={12} tickLine={false} />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '0.75rem', color: '#fff' }}
                  formatter={(value: any) => [`€${value}`, 'Volume']}
                />
                <Area type="monotone" dataKey="volume" stroke="#6366f1" strokeWidth={3} fillOpacity={1} fill="url(#colorVolume)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Ultime Transazioni Widget */}
        <div className="glass-panel p-6 space-y-4 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h2 className="text-lg font-bold text-white">Ultime Transazioni</h2>
              <Clock className="w-4 h-4 text-slate-400" />
            </div>

            <div className="mt-4 space-y-3">
              {loading ? (
                <div className="text-xs text-slate-500 py-8 text-center">Caricamento transazioni...</div>
              ) : payments.length === 0 ? (
                <div className="text-xs text-slate-500 py-8 text-center">Nessuna transazione trovata</div>
              ) : (
                payments.slice(0, 4).map((p) => (
                  <div key={p.id} className="glass-card p-3 flex items-center justify-between">
                    <div>
                      <div className="text-xs font-semibold text-slate-200">{p.description || 'Pagamento Direct API'}</div>
                      <div className="text-[10px] text-slate-400 mt-0.5">{p.id} • {new Date(p.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</div>
                    </div>
                    <div className="text-right">
                      <div className="text-xs font-bold text-white">€{(p.amountCents / 100).toFixed(2)}</div>
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full inline-block mt-0.5 ${
                        p.status === 'SUCCEEDED' ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' :
                        p.status === 'FAILED' ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30' :
                        p.status === 'PARTIALLY_REFUNDED' ? 'bg-purple-500/20 text-purple-400 border border-purple-500/30' :
                        'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                      }`}>
                        {p.status}
                      </span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          <a href="#/transactions" className="btn-secondary w-full text-center text-xs font-semibold py-2.5 mt-4 block">
            Vedi tutte le transazioni →
          </a>
        </div>
      </div>
    </div>
  );
};
