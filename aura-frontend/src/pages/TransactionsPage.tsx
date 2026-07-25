import React, { useEffect, useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { paymentApi } from '../api/paymentApi';
import type { PaymentIntent } from '../types';
import { 
  Search, 
  Filter, 
  CheckCircle2, 
  XCircle, 
  RotateCcw, 
  X, 
  Eye
} from 'lucide-react';
import { toast } from 'sonner';

export const TransactionsPage: React.FC = () => {
  const { merchant, isTest } = useMerchant();
  const [payments, setPayments] = useState<PaymentIntent[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  // Selected Transaction for Drawer Detail
  const [selectedTx, setSelectedTx] = useState<PaymentIntent | null>(null);

  // Refund Modal State
  const [refundModalTx, setRefundModalTx] = useState<PaymentIntent | null>(null);
  const [refundAmount, setRefundAmount] = useState<string>('');
  const [refundReason, setRefundReason] = useState<string>('Richiesta cliente per reso prodotto');
  const [isRefunding, setIsRefunding] = useState<boolean>(false);

  useEffect(() => {
    const fetchPayments = async () => {
      setLoading(true);
      try {
        const data = await paymentApi.getPayments({ isTest });
        setPayments(data);
      } catch (err) {
        toast.error('Errore nel caricamento delle transazioni');
      } finally {
        setLoading(false);
      }
    };
    fetchPayments();
  }, [merchant.id, isTest]);

  const formatCurrency = (cents: number) => {
    return new Intl.NumberFormat('it-IT', { style: 'currency', currency: 'EUR' }).format(cents / 100);
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('it-IT', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const filteredPayments = payments.filter((p) => {
    const email = p.customerEmail || '';
    const desc = p.description || '';
    const matchesSearch = 
      p.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
      email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      desc.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesStatus = statusFilter === 'ALL' || p.status === statusFilter;
    const matchesEnv = isTest ? p.isTest : !p.isTest;

    return matchesSearch && matchesStatus && matchesEnv;
  });

  const openRefundModal = (tx: PaymentIntent) => {
    const remainingCents = tx.amountCents - (tx.refundedAmountCents || 0);
    setRefundModalTx(tx);
    setRefundAmount((remainingCents / 100).toFixed(2));
  };

  const handleRefundSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!refundModalTx) return;

    const refundCents = Math.round(parseFloat(refundAmount) * 100);
    const maxRefundable = refundModalTx.amountCents - (refundModalTx.refundedAmountCents || 0);

    if (refundCents <= 0 || refundCents > maxRefundable) {
      toast.error(`Importo rimborso non valido. Massimo rimborsabile: ${formatCurrency(maxRefundable)}`);
      return;
    }

    setIsRefunding(true);
    try {
      await paymentApi.refundPayment(refundModalTx.id, {
        amountCents: refundCents,
        reason: refundReason,
      });

      setPayments((prev) =>
        prev.map((p) => {
          if (p.id === refundModalTx.id) {
            const newRefunded = (p.refundedAmountCents || 0) + refundCents;
            const isTotal = newRefunded >= p.amountCents;
            return {
              ...p,
              status: isTotal ? 'REFUNDED' : 'PARTIALLY_REFUNDED',
              refundedAmountCents: newRefunded,
            };
          }
          return p;
        })
      );

      toast.success(`Rimborso di ${formatCurrency(refundCents)} eseguito con successo!`);
      setRefundModalTx(null);
    } catch (err) {
      toast.error('Errore durante l\'esecuzione del rimborso');
    } finally {
      setIsRefunding(false);
    }
  };

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Title */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
            Registro Transazioni
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Visualizza lo storico dei pagamenti, ispeziona la timeline degli eventi ed esegui rimborsi.
          </p>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="glass-panel p-4 flex flex-col md:flex-row gap-4 justify-between items-center">
        <div className="relative w-full md:w-96">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <input
            type="text"
            placeholder="Cerca per email cliente, ID o descrizione..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="glass-input pl-9 w-full text-xs"
          />
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto justify-end">
          <div className="flex items-center gap-2 text-xs text-slate-400">
            <Filter className="w-4 h-4" />
            <span>Filtra per stato:</span>
          </div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="glass-input text-xs cursor-pointer"
          >
            <option value="ALL">Tutti gli stati</option>
            <option value="SUCCEEDED">Autorizzato / Completato</option>
            <option value="FAILED">Rifiutato / Fallito</option>
            <option value="PARTIALLY_REFUNDED">Parzialmente Rimborsato</option>
            <option value="REFUNDED">Rimborsato Totale</option>
          </select>
        </div>
      </div>

      {/* Transactions Table */}
      <div className="glass-panel overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-[11px] font-semibold text-slate-400 uppercase tracking-wider bg-slate-900/50">
                <th className="py-4 px-6">ID Transazione</th>
                <th className="py-4 px-6">Cliente & Descrizione</th>
                <th className="py-4 px-6">Importo</th>
                <th className="py-4 px-6">Stato</th>
                <th className="py-4 px-6">Data</th>
                <th className="py-4 px-6 text-right">Azioni</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-xs">
              {loading ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-500">
                    Caricamento transazioni in corso...
                  </td>
                </tr>
              ) : filteredPayments.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-500">
                    Nessuna transazione trovata per i filtri selezionati.
                  </td>
                </tr>
              ) : (
                filteredPayments.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-900/40 transition-colors">
                    <td className="py-4 px-6 font-mono font-semibold text-indigo-400">{p.id}</td>
                    <td className="py-4 px-6">
                      <div className="font-semibold text-slate-100">{p.description || 'Pagamento Online'}</div>
                      <div className="text-[11px] text-slate-400">{p.customerEmail || 'cliente@mail.com'}</div>
                    </td>
                    <td className="py-4 px-6 font-mono font-bold text-slate-100">
                      {formatCurrency(p.amountCents)}
                      {p.refundedAmountCents && p.refundedAmountCents > 0 ? (
                        <div className="text-[10px] text-rose-400 font-medium">
                          - {formatCurrency(p.refundedAmountCents)} (Rimborsati)
                        </div>
                      ) : null}
                    </td>
                    <td className="py-4 px-6">
                      <span className={`text-[10px] font-bold px-2.5 py-1 rounded-full inline-flex items-center gap-1.5 ${
                        p.status === 'SUCCEEDED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30' :
                        p.status === 'FAILED' ? 'bg-rose-500/10 text-rose-400 border border-rose-500/30' :
                        'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                      }`}>
                        {p.status === 'SUCCEEDED' && <CheckCircle2 className="w-3 h-3" />}
                        {p.status === 'FAILED' && <XCircle className="w-3 h-3" />}
                        {(p.status === 'REFUNDED' || p.status === 'PARTIALLY_REFUNDED') && <RotateCcw className="w-3 h-3" />}
                        {p.status === 'SUCCEEDED' ? 'APPROVATO' :
                         p.status === 'FAILED' ? 'RIFIUTATO' :
                         p.status === 'PARTIALLY_REFUNDED' ? 'PARZ. RIMBORSATO' : 'RIMBORSATO'}
                      </span>
                    </td>
                    <td className="py-4 px-6 text-slate-400">{formatDate(p.createdAt)}</td>
                    <td className="py-4 px-6 text-right space-x-2">
                      <button
                        onClick={() => setSelectedTx(p)}
                        className="p-2 rounded-lg bg-slate-900 hover:bg-slate-800 text-slate-300 hover:text-white transition-colors"
                        title="Dettaglio Timeline"
                      >
                        <Eye className="w-4 h-4" />
                      </button>

                      {(p.status === 'SUCCEEDED' || p.status === 'PARTIALLY_REFUNDED') && (
                        <button
                          onClick={() => openRefundModal(p)}
                          className="px-3 py-1.5 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 text-xs font-medium transition-colors inline-flex items-center gap-1"
                        >
                          <RotateCcw className="w-3 h-3" />
                          <span>Rimborsa</span>
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Transaction Detail Drawer */}
      {selectedTx && (
        <div className="fixed inset-0 z-50 flex justify-end bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
          <div className="w-full max-w-md bg-slate-900 border-l border-slate-800 p-6 flex flex-col justify-between h-full space-y-6">
            <div className="space-y-6 overflow-y-auto">
              <div className="flex items-center justify-between pb-4 border-b border-slate-800">
                <div>
                  <h2 className="text-lg font-bold text-white">Dettaglio Transazione</h2>
                  <p className="font-mono text-xs text-indigo-400">{selectedTx.id}</p>
                </div>
                <button onClick={() => setSelectedTx(null)} className="text-slate-400 hover:text-white p-1">
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* General Metadata */}
              <div className="space-y-3 text-xs">
                <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
                  <div className="flex justify-between text-slate-400">
                    <span>Importo Totale:</span>
                    <span className="font-mono font-bold text-slate-100 text-sm">{formatCurrency(selectedTx.amountCents)}</span>
                  </div>
                  <div className="flex justify-between text-slate-400">
                    <span>Cliente:</span>
                    <span className="text-slate-200 font-semibold">{selectedTx.customerEmail || 'cliente@mail.com'}</span>
                  </div>
                  <div className="flex justify-between text-slate-400">
                    <span>Codice Autorizzazione:</span>
                    <span className="font-mono text-slate-300">{selectedTx.authorizationCode || 'AUTH_891234'}</span>
                  </div>
                </div>

                {/* Timeline Eventi */}
                <h3 className="font-bold text-slate-200 pt-2">Timeline Ciclo di Vita</h3>
                <div className="space-y-3">
                  <div className="flex gap-3 text-xs">
                    <div className="w-2.5 h-2.5 rounded-full bg-indigo-500 mt-1" />
                    <div>
                      <div className="font-semibold text-slate-200">PaymentIntent Creato</div>
                      <div className="text-[10px] text-slate-400">{formatDate(selectedTx.createdAt)}</div>
                    </div>
                  </div>
                  <div className="flex gap-3 text-xs">
                    <div className={`w-2.5 h-2.5 rounded-full ${selectedTx.status === 'FAILED' ? 'bg-rose-500' : 'bg-emerald-500'} mt-1`} />
                    <div>
                      <div className="font-semibold text-slate-200">
                        {selectedTx.status === 'FAILED' ? 'Autorizzazione Rifiutata' : 'Autorizzazione Completata (00)'}
                      </div>
                      <div className="text-[10px] text-slate-400">{formatDate(selectedTx.updatedAt)}</div>
                      {selectedTx.failureReason && (
                        <div className="text-[11px] text-rose-400 mt-1">{selectedTx.failureReason}</div>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <button onClick={() => setSelectedTx(null)} className="btn-secondary w-full text-xs font-semibold py-2.5">
              Chiudi Dettaglio
            </button>
          </div>
        </div>
      )}

      {/* Refund Modal */}
      {refundModalTx && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fadeIn">
          <div className="glass-panel max-w-md w-full p-6 space-y-6 border border-slate-800">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <RotateCcw className="w-5 h-5 text-rose-400" />
                <h2 className="text-lg font-bold text-white">Esegui Rimborso</h2>
              </div>
              <button onClick={() => setRefundModalTx(null)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleRefundSubmit} className="space-y-4 text-xs">
              <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 space-y-1">
                <div className="text-slate-400">Transazione: <span className="font-mono text-slate-200">{refundModalTx.id}</span></div>
                <div className="text-slate-400">Importo Originale: <span className="font-mono text-slate-200">{formatCurrency(refundModalTx.amountCents)}</span></div>
                <div className="text-emerald-400 font-semibold">
                  Disponibile per Rimborso: {formatCurrency(refundModalTx.amountCents - (refundModalTx.refundedAmountCents || 0))}
                </div>
              </div>

              <div>
                <label className="block text-slate-300 font-semibold mb-1">Importo da Rimborsare (€)</label>
                <input
                  type="number"
                  step="0.01"
                  value={refundAmount}
                  onChange={(e) => setRefundAmount(e.target.value)}
                  className="glass-input w-full font-bold text-base text-rose-400"
                  required
                />
              </div>

              <div>
                <label className="block text-slate-300 font-semibold mb-1">Motivo del Rimborso</label>
                <input
                  type="text"
                  value={refundReason}
                  onChange={(e) => setRefundReason(e.target.value)}
                  className="glass-input w-full"
                  required
                />
              </div>

              <div className="pt-2 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setRefundModalTx(null)}
                  className="btn-secondary text-xs px-4 py-2"
                >
                  Annulla
                </button>
                <button
                  type="submit"
                  disabled={isRefunding}
                  className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-semibold text-xs transition-colors"
                >
                  {isRefunding ? 'Esecuzione...' : 'Conferma Rimborso'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
