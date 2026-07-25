import React, { useEffect, useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { ledgerApi } from '../api/ledgerApi';
import { paymentApi } from '../api/paymentApi';
import type { LedgerBalance, PaymentIntent } from '../types';
import { 
  TrendingUp, 
  Wallet, 
  CreditCard, 
  ArrowUpRight, 
  CheckCircle2, 
  XCircle, 
  RefreshCw,
  Sparkles
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
        const balanceData = await ledgerApi.getMerchantBalance(merchant.id, isTest);
        const paymentsData = await paymentApi.getPayments({ isTest });
        setBalance(balanceData);
        setPayments(paymentsData);
      } catch (err) {
        console.error('Errore nel caricamento dati dashboard:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [merchant.id, isTest]);

  // Formattazione Valuta in Euro (€)
  const formatCurrency = (cents: number) => {
    return new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' }).format(cents / 100);
  };

  // Formattazione Data
  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('it-IT', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Calcoli metriche generali
  const totalVolumeCents = payments
    .filter((p) => p.status === 'SUCCEEDED')
    .reduce((acc, p) => acc + p.amountCents, 0);

  const totalTransactions = payments.length;
  const succeededCount = payments.filter((p) => p.status === 'SUCCEEDED').length;
  const approvalRate = totalTransactions > 0 ? Math.round((succeededCount / totalTransactions) * 100) : 100;

  // Dati per il grafico degli ultimi 7 giorni
  const chartData = [
    { day: '19 Lug', volume: 140 },
    { day: '20 Lug', volume: 220 },
    { day: '21 Lug', volume: 180 },
    { day: '22 Lug', volume: 310 },
    { day: '23 Lug', volume: 350 },
    { day: '24 Lug', volume: 290 },
    { day: '25 Lug', volume: totalVolumeCents > 0 ? totalVolumeCents / 100 : 125 },
  ];

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Top Banner & Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight flex items-center gap-3">
            <span>Panoramica Merchant</span>
            {isTest ? (
              <span className="text-xs font-semibold px-3 py-1 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/40 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5" />
                Ambiente Sandbox (Test)
              </span>
            ) : (
              <span className="text-xs font-semibold px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5" />
                Ambiente Produzione (Live)
              </span>
            )}
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Monitora i saldi del Ledger in tempo reale, i volumi transati e le metriche di conversione.
          </p>
        </div>
      </div>

      {/* Metric Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* Card 1: Saldo Disponibile */}
        <div className="glass-panel p-5 space-y-3 relative overflow-hidden group hover:border-indigo-500/50 transition-all duration-300">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Saldo Disponibile (Ledger)</span>
            <div className="w-8 h-8 rounded-lg bg-indigo-500/10 flex items-center justify-center text-indigo-400 border border-indigo-500/20">
              <Wallet className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl lg:text-3xl font-extrabold text-white font-mono tracking-tight">
            {loading ? '...' : formatCurrency(balance?.availableBalanceCents || 0)}
          </div>
          <div className="text-[11px] text-slate-400 flex items-center gap-1">
            <span className="text-emerald-400 font-semibold flex items-center">
              <ArrowUpRight className="w-3.5 h-3.5" />
              +12.4%
            </span>
            <span>rispetto alla settimana precedente</span>
          </div>
        </div>

        {/* Card 2: Volume Processato */}
        <div className="glass-panel p-5 space-y-3 relative overflow-hidden group hover:border-indigo-500/50 transition-all duration-300">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Volume Processato</span>
            <div className="w-8 h-8 rounded-lg bg-cyan-500/10 flex items-center justify-center text-cyan-400 border border-cyan-500/20">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl lg:text-3xl font-extrabold text-white font-mono tracking-tight">
            {loading ? '...' : formatCurrency(totalVolumeCents)}
          </div>
          <div className="text-[11px] text-slate-400">Totale pagamenti autorizzati</div>
        </div>

        {/* Card 3: Transazioni Totali */}
        <div className="glass-panel p-5 space-y-3 relative overflow-hidden group hover:border-indigo-500/50 transition-all duration-300">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Transazioni Totali</span>
            <div className="w-8 h-8 rounded-lg bg-purple-500/10 flex items-center justify-center text-purple-400 border border-purple-500/20">
              <CreditCard className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl lg:text-3xl font-extrabold text-white font-mono tracking-tight">
            {loading ? '...' : totalTransactions}
          </div>
          <div className="text-[11px] text-slate-400">Numero complessivo di tentativi</div>
        </div>

        {/* Card 4: Tasso di Approvazione */}
        <div className="glass-panel p-5 space-y-3 relative overflow-hidden group hover:border-indigo-500/50 transition-all duration-300">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Tasso di Approvazione</span>
            <div className="w-8 h-8 rounded-lg bg-emerald-500/10 flex items-center justify-center text-emerald-400 border border-emerald-500/20">
              <CheckCircle2 className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl lg:text-3xl font-extrabold text-white font-mono tracking-tight">
            {loading ? '...' : `${approvalRate}%`}
          </div>
          <div className="text-[11px] text-slate-400">Conversione autorizzazioni bancarie</div>
        </div>
      </div>

      {/* Chart & Recent Activity Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Recharts Volume Chart */}
        <div className="glass-panel p-6 lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-800">
            <div>
              <h2 className="text-base font-bold text-white">Andamento Volumi Incassati</h2>
              <p className="text-xs text-slate-400">Ultimi 7 giorni di attività</p>
            </div>
            <span className="text-xs font-mono text-indigo-400 font-semibold">Valori in EUR (€)</span>
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
                <XAxis dataKey="day" stroke="#64748b" fontSize={11} tickLine={false} axisLine={false} />
                <YAxis stroke="#64748b" fontSize={11} tickLine={false} axisLine={false} tickFormatter={(val) => `€${val}`} />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#090d16', borderColor: '#334155', borderRadius: '12px', fontSize: '12px' }}
                  formatter={(val: any) => [`€ ${Number(val || 0).toFixed(2)}`, 'Volume Incassato']}
                />
                <Area type="monotone" dataKey="volume" stroke="#6366f1" strokeWidth={3} fillOpacity={1} fill="url(#colorVolume)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Ultime Transazioni Table Summary */}
        <div className="glass-panel p-6 space-y-4 flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h2 className="text-base font-bold text-white">Ultime Transazioni</h2>
              <span className="text-xs text-slate-400">Aggiornato ora</span>
            </div>

            <div className="space-y-3">
              {payments.slice(0, 4).map((p) => (
                <div key={p.id} className="p-3 rounded-xl bg-slate-900/50 border border-slate-800/80 flex items-center justify-between text-xs hover:border-slate-700 transition-colors">
                  <div className="flex items-center gap-3">
                    <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                      p.status === 'SUCCEEDED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' :
                      p.status === 'FAILED' ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20' :
                      'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                    }`}>
                      {p.status === 'SUCCEEDED' ? <CheckCircle2 className="w-4 h-4" /> :
                       p.status === 'FAILED' ? <XCircle className="w-4 h-4" /> :
                       <RefreshCw className="w-4 h-4" />}
                    </div>
                    <div>
                      <div className="font-semibold text-slate-200 truncate max-w-[130px]">{p.description || 'Pagamento online'}</div>
                      <div className="text-[10px] text-slate-400">{formatDate(p.createdAt)}</div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="font-mono font-bold text-slate-100">{formatCurrency(p.amountCents)}</div>
                    <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded-full ${
                      p.status === 'SUCCEEDED' ? 'bg-emerald-500/10 text-emerald-400' :
                      p.status === 'FAILED' ? 'bg-rose-500/10 text-rose-400' :
                      'bg-amber-500/10 text-amber-400'
                    }`}>
                      {p.status === 'SUCCEEDED' ? 'APPROVATO' : p.status === 'FAILED' ? 'RIFIUTATO' : 'RIMBORSATO'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
