import React, { useEffect, useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { ledgerApi } from '../api/ledgerApi';
import type { LedgerBalance, LedgerEntry } from '../types';
import { 
  Scale, 
  Wallet, 
  Building2, 
  ArrowUpRight, 
  ArrowDownLeft, 
  RefreshCw,
  Info,
  ShieldCheck,
  CheckCircle2
} from 'lucide-react';
import { toast } from 'sonner';

export const LedgerPage: React.FC = () => {
  const { merchant, isTest } = useMerchant();
  const [balance, setBalance] = useState<LedgerBalance | null>(null);
  const [entries, setEntries] = useState<LedgerEntry[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [accountFilter, setAccountFilter] = useState<string>('ALL');

  const fetchLedgerData = async () => {
    setLoading(true);
    try {
      const [balRes, entriesRes] = await Promise.all([
        ledgerApi.getMerchantBalance(merchant.id, isTest),
        ledgerApi.getLedgerEntries(merchant.id, isTest),
      ]);
      setBalance(balRes);
      setEntries(entriesRes);
    } catch (err) {
      toast.error('Errore nel caricamento dei dati contabili del Ledger');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLedgerData();
  }, [merchant.id, isTest]);

  const formatCurrency = (cents: number) => {
    return new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' }).format(cents / 100);
  };

  const formatDate = (dateStr: string) => {
    if (!dateStr) return 'Ora';
    return new Date(dateStr).toLocaleDateString('it-IT', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  const filteredEntries = entries.filter((e) => {
    if (accountFilter === 'ALL') return true;
    return e.accountType === accountFilter;
  });

  // Calcolo totale movimenti DARE e AVERE
  const totalDebit = filteredEntries
    .filter(e => e.entryType === 'DEBIT')
    .reduce((sum, e) => sum + e.amountCents, 0);

  const totalCredit = filteredEntries
    .filter(e => e.entryType === 'CREDIT')
    .reduce((sum, e) => sum + e.amountCents, 0);

  return (
    <div className="space-y-6 max-w-6xl animate-fadeIn">
      {/* Header Page Title */}
      <div className="border-b border-zinc-200 pb-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-zinc-900 tracking-tight flex items-center gap-2">
            <Scale className="w-5 h-5 text-indigo-600" />
            <span>Ledger Contabile a Partita Doppia</span>
          </h1>
          <p className="text-zinc-500 text-xs mt-0.5">
            Mastro contabile multi-tenant ad immutabilità garantita con bilanciamento algebrico DARE / AVERE (<code className="font-mono text-zinc-700">aura-ledger-service</code>).
          </p>
        </div>

        <button
          onClick={fetchLedgerData}
          disabled={loading}
          className="btn-shadcn-secondary text-xs px-3 py-1.5 flex items-center gap-1.5 self-start md:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Aggiorna Mastro</span>
        </button>
      </div>

      {/* Guida Semplificata alla Partita Doppia Contabile */}
      <div className="p-4 rounded-lg bg-indigo-50 border border-indigo-200 text-indigo-900 text-xs space-y-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 font-bold text-indigo-950">
            <ShieldCheck className="w-4 h-4 text-indigo-600" />
            <span>Guida al Mastro Contabile a Partita Doppia (Inviolabilità dei Fondi)</span>
          </div>
          <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-indigo-100 text-indigo-800 border border-indigo-300">
            GARANZIA CONTABILE
          </span>
        </div>
        <p className="text-indigo-900 leading-relaxed text-[11px]">
          In AuraPay nessun saldo contabile viene mai modificato o cancellato a mano. Ogni volta che un cliente paga o riceve un rimborso, il sistema registra due righe contabili bilanciate: il movimento in <strong>DARE (DEBIT)</strong> ed il corrispettivo in <strong>AVERE (CREDIT)</strong>. La somma algebrica è sempre pari a ZERO (&Sigma; &Delta; = 0), garantendo che nessun centesimo possa mai andare smarrito.
        </p>
      </div>

      {/* KPI Cards Multi-Tenant Balance */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        
        {/* Card 1: Disponibile Esercente */}
        <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs space-y-2">
          <div className="flex items-center justify-between text-zinc-500 text-xs">
            <span className="font-medium flex items-center gap-1.5">
              <Wallet className="w-4 h-4 text-emerald-600" />
              <span>Saldo Disponibile Esercente</span>
            </span>
            <span className="text-[10px] font-mono text-zinc-400">MERCHANT_AVAILABLE</span>
          </div>
          <div className="text-2xl font-bold text-zinc-900 tracking-tight font-mono">
            {balance ? formatCurrency(balance.availableBalanceCents) : '€ 0,00'}
          </div>
          <p className="text-[11px] text-zinc-500 flex items-center gap-1 pt-1 border-t border-zinc-100">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
            <span>Esercente: <strong>{merchant.businessName}</strong> ({isTest ? 'SANDBOX' : 'LIVE'})</span>
          </p>
        </div>

        {/* Card 2: Riserva di Regolamento */}
        <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs space-y-2">
          <div className="flex items-center justify-between text-zinc-500 text-xs">
            <span className="font-medium flex items-center gap-1.5">
              <Building2 className="w-4 h-4 text-indigo-600" />
              <span>Riserva Regolamento Bancario</span>
            </span>
            <span className="text-[10px] font-mono text-zinc-400">SETTLEMENT_HOLDING</span>
          </div>
          <div className="text-2xl font-bold text-zinc-900 tracking-tight font-mono">
            {formatCurrency(totalCredit - totalDebit)}
          </div>
          <p className="text-[11px] text-zinc-500 flex items-center gap-1 pt-1 border-t border-zinc-100">
            <Info className="w-3.5 h-3.5 text-indigo-600" />
            <span>Fondi trattenuti fino al Settlement bancario</span>
          </p>
        </div>

        {/* Card 3: Commissioni Piattaforma */}
        <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs space-y-2">
          <div className="flex items-center justify-between text-zinc-500 text-xs">
            <span className="font-medium flex items-center gap-1.5">
              <ArrowUpRight className="w-4 h-4 text-purple-600" />
              <span>Commissioni Piattaforma AuraPay</span>
            </span>
            <span className="text-[10px] font-mono text-zinc-400">SYSTEM_REVENUE</span>
          </div>
          <div className="text-2xl font-bold text-zinc-900 tracking-tight font-mono">
            {formatCurrency(entries.filter(e => e.accountType === 'SYSTEM_REVENUE').reduce((sum, e) => sum + e.amountCents, 0))}
          </div>
          <p className="text-[11px] text-zinc-500 flex items-center gap-1 pt-1 border-t border-zinc-100">
            <CheckCircle2 className="w-3.5 h-3.5 text-purple-600" />
            <span>Fee trattenute automaticamente sulle transazioni</span>
          </p>
        </div>

      </div>

      {/* Tabella Scritture Contabili Mastro */}
      <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-zinc-100">
          <div className="flex items-center gap-2">
            <h2 className="text-xs font-bold text-zinc-900 uppercase tracking-wider">
              Scritture Contabili Mastro ({filteredEntries.length})
            </h2>
            <span className="text-[10px] font-mono font-semibold px-2 py-0.5 rounded bg-zinc-100 text-zinc-600 border border-zinc-200">
              Merchant: {merchant.businessName}
            </span>
          </div>

          {/* Filtro Tipo Conto */}
          <div className="flex items-center gap-2">
            <span className="text-xs text-zinc-500">Conto:</span>
            <select
              value={accountFilter}
              onChange={(e) => setAccountFilter(e.target.value)}
              className="shadcn-input text-xs py-1 px-2 font-mono"
            >
              <option value="ALL">Tutti i Conti</option>
              <option value="MERCHANT_AVAILABLE">MERCHANT_AVAILABLE</option>
              <option value="SETTLEMENT_HOLDING">SETTLEMENT_HOLDING</option>
              <option value="SYSTEM_REVENUE">SYSTEM_REVENUE</option>
            </select>
          </div>
        </div>

        {loading ? (
          <div className="py-12 text-center text-zinc-400 text-xs">
            <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-indigo-600" />
            <span>Caricamento libro mastro in corso...</span>
          </div>
        ) : filteredEntries.length === 0 ? (
          <div className="py-12 text-center text-zinc-400 text-xs">
            Nessuna scrittura contabile registrata per l'esercente <strong>{merchant.businessName}</strong> in ambiente {isTest ? 'SANDBOX' : 'LIVE'}.
            <br />
            <span className="text-zinc-500 text-[11px] block mt-1">Esegui un pagamento dal Checkout Demo per generare le scritture contabili in Partita Doppia!</span>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-zinc-200 text-zinc-400 text-[11px] uppercase tracking-wider bg-zinc-50/50">
                  <th className="py-2.5 px-3">Data / Ora</th>
                  <th className="py-2.5 px-3">Conto Impattato</th>
                  <th className="py-2.5 px-3">Scrittura (Dare / Avere)</th>
                  <th className="py-2.5 px-3 font-right">Importo</th>
                  <th className="py-2.5 px-3">Riferimento Transazione</th>
                  <th className="py-2.5 px-3 text-right">Ambiente</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100 font-mono">
                {filteredEntries.map((entry, index) => {
                  const isDebit = entry.entryType === 'DEBIT';
                  const rowKey = entry.id ? `entry-${entry.id}` : `ledger-idx-${index}`;
                  return (
                    <tr key={rowKey} className="hover:bg-zinc-50/80 transition-colors">
                      <td className="py-3 px-3 text-zinc-600 font-sans text-xs">
                        {formatDate(entry.createdAt)}
                      </td>

                      <td className="py-3 px-3 font-semibold text-zinc-800 text-[11px]">
                        {entry.accountType}
                      </td>

                      <td className="py-3 px-3">
                        <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold border ${
                          isDebit 
                            ? 'bg-rose-50 text-rose-700 border-rose-200' 
                            : 'bg-emerald-50 text-emerald-700 border-emerald-200'
                        }`}>
                          {isDebit ? <ArrowUpRight className="w-3 h-3 text-rose-600" /> : <ArrowDownLeft className="w-3 h-3 text-emerald-600" />}
                          <span>{isDebit ? 'DARE (DEBIT)' : 'AVERE (CREDIT)'}</span>
                        </span>
                      </td>

                      <td className="py-3 px-3 font-bold text-zinc-900 text-xs">
                        {isDebit ? '-' : '+'}{formatCurrency(entry.amountCents)}
                      </td>

                      <td className="py-3 px-3 text-zinc-500 text-[11px]">
                        {entry.paymentIntentId ? (
                          <span className="text-indigo-600">PI: {entry.paymentIntentId.substring(0, 18)}...</span>
                        ) : entry.refundId ? (
                          <span className="text-purple-600">REF: {entry.refundId.substring(0, 18)}...</span>
                        ) : (
                          'N/A'
                        )}
                      </td>

                      <td className="py-3 px-3 text-right">
                        <span className={`text-[9px] px-1.5 py-0.5 rounded font-bold border ${
                          entry.isTest ? 'bg-amber-50 text-amber-700 border-amber-200' : 'bg-emerald-50 text-emerald-700 border-emerald-200'
                        }`}>
                          {entry.isTest ? 'SANDBOX' : 'LIVE'}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
