import React, { useEffect, useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { ledgerApi } from '../api/ledgerApi';
import { paymentApi } from '../api/paymentApi';
import type { LedgerBalance, PaymentIntent } from '../types';
import { 
  TrendingUp, 
  Wallet, 
  CreditCard, 
  CheckCircle2, 
  XCircle, 
  RefreshCw
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

  const formatCurrency = (cents: number) => {
    return new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' }).format(cents / 100);
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('it-IT', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const totalVolumeCents = payments
    .filter((p) => p.status === 'SUCCEEDED')
    .reduce((acc, p) => acc + p.amountCents, 0);

  const totalTransactions = payments.length;
  const succeededCount = payments.filter((p) => p.status === 'SUCCEEDED').length;
  const approvalRate = totalTransactions > 0 ? Math.round((succeededCount / totalTransactions) * 100) : 100;

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
      {/* Title */}
      <div className="border-b border-zinc-200 pb-5">
        <h1 className="text-xl font-bold text-zinc-900 tracking-tight">
          Panoramica Attività
        </h1>
        <p className="text-zinc-500 text-xs mt-0.5">
          Monitoraggio saldi Ledger, volumi processati e tasso di autorizzazione bancaria.
        </p>
      </div>

      {/* Metric Cards Grid (Shadcn Light style) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Saldo Disponibile */}
        <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs space-y-2">
          <div className="flex items-center justify-between text-zinc-500">
            <span className="text-xs font-semibold uppercase tracking-wider">Saldo Disponibile</span>
            <Wallet className="w-4 h-4 text-zinc-400" />
          </div>
          <div className="text-2xl font-bold text-zinc-900 font-mono">
            {loading ? '...' : formatCurrency(balance?.availableBalanceCents || 0)}
          </div>
          <p className="text-[11px] text-zinc-500">Aggiornato dal Ledger contabile</p>
        </div>

        {/* Card 2: Volume Processato */}
        <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs space-y-2">
          <div className="flex items-center justify-between text-zinc-500">
            <span className="text-xs font-semibold uppercase tracking-wider">Volume Processato</span>
            <TrendingUp className="w-4 h-4 text-zinc-400" />
          </div>
          <div className="text-2xl font-bold text-zinc-900 font-mono">
            {loading ? '...' : formatCurrency(totalVolumeCents)}
          </div>
          <p className="text-[11px] text-zinc-500">Totale pagamenti autorizzati</p>
        </div>

        {/* Card 3: Transazioni Totali */}
        <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs space-y-2">
          <div className="flex items-center justify-between text-zinc-500">
            <span className="text-xs font-semibold uppercase tracking-wider">Transazioni Totali</span>
            <CreditCard className="w-4 h-4 text-zinc-400" />
          </div>
          <div className="text-2xl font-bold text-zinc-900 font-mono">
            {loading ? '...' : totalTransactions}
          </div>
          <p className="text-[11px] text-zinc-500">Numero complessivo tentativi</p>
        </div>

        {/* Card 4: Tasso di Approvazione */}
        <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs space-y-2">
          <div className="flex items-center justify-between text-zinc-500">
            <span className="text-xs font-semibold uppercase tracking-wider">Tasso di Approvazione</span>
            <CheckCircle2 className="w-4 h-4 text-zinc-400" />
          </div>
          <div className="text-2xl font-bold text-zinc-900 font-mono">
            {loading ? '...' : `${approvalRate}%`}
          </div>
          <div className="text-[11px] text-zinc-500">Conversione autorizzazioni</div>
        </div>
      </div>

      {/* Chart & Recent Activity Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recharts Volume Chart */}
        <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-zinc-100">
            <h2 className="text-sm font-semibold text-zinc-900">Andamento Volumi</h2>
            <span className="text-xs font-mono text-zinc-500">Valori in EUR (€)</span>
          </div>

          <div className="h-60 w-full pt-2">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="colorVolume" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#18181b" stopOpacity={0.2}/>
                    <stop offset="95%" stopColor="#18181b" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <XAxis dataKey="day" stroke="#a1a1aa" fontSize={11} tickLine={false} axisLine={false} />
                <YAxis stroke="#a1a1aa" fontSize={11} tickLine={false} axisLine={false} tickFormatter={(val) => `€${val}`} />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#ffffff', borderColor: '#e4e4e7', borderRadius: '8px', fontSize: '12px', color: '#09090b', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)' }}
                  formatter={(val: any) => [`€ ${Number(val || 0).toFixed(2)}`, 'Volume Incassato']}
                />
                <Area type="monotone" dataKey="volume" stroke="#18181b" strokeWidth={2} fillOpacity={1} fill="url(#colorVolume)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Ultime Transazioni Table Summary */}
        <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-zinc-100">
            <h2 className="text-sm font-semibold text-zinc-900">Ultime Transazioni</h2>
          </div>

          <div className="space-y-2.5">
            {(Array.isArray(payments) ? payments : []).slice(0, 4).map((p, idx) => (
              <div key={p.id ? `dash-p-${p.id}` : `dash-p-idx-${idx}`} className="p-3 rounded border border-zinc-200 bg-zinc-50 flex items-center justify-between text-xs">
                <div className="flex items-center gap-2.5">
                  <div>
                    {p.status === 'SUCCEEDED' ? <CheckCircle2 className="w-4 h-4 text-emerald-600" /> :
                     p.status === 'FAILED' ? <XCircle className="w-4 h-4 text-rose-600" /> :
                     <RefreshCw className="w-4 h-4 text-amber-600" />}
                  </div>
                  <div>
                    <div className="font-medium text-zinc-900 truncate max-w-[130px]">{p.description || 'Pagamento online'}</div>
                    <div className="text-[10px] text-zinc-500">{formatDate(p.createdAt)}</div>
                  </div>
                </div>
                <div className="text-right">
                  <div className="font-mono font-bold text-zinc-900">{formatCurrency(p.amountCents)}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
