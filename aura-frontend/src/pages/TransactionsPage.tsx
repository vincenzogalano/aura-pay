import React, { useEffect, useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { paymentApi } from '../api/paymentApi';
import type { PaymentIntent } from '../types';
import { 
  Search, 
  Filter, 
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

  const [selectedTx, setSelectedTx] = useState<PaymentIntent | null>(null);

  const [refundModalTx, setRefundModalTx] = useState<PaymentIntent | null>(null);
  const [refundAmount, setRefundAmount] = useState<string>('');
  const [refundReason, setRefundReason] = useState<string>('Reso prodotto');
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
      toast.error(`Importo non valido. Massimo rimborsabile: ${formatCurrency(maxRefundable)}`);
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

      toast.success(`Rimborso di ${formatCurrency(refundCents)} completato!`);
      setRefundModalTx(null);
    } catch (err) {
      toast.error('Errore durante l\'esecuzione del rimborso');
    } finally {
      setIsRefunding(false);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Title */}
      <div className="border-b border-zinc-200 pb-5">
        <h1 className="text-xl font-bold text-zinc-900 tracking-tight">
          Transazioni
        </h1>
        <p className="text-zinc-500 text-xs mt-0.5">
          Storico dei pagamenti autorizzati ed elaborazione storni/rimborsi.
        </p>
      </div>

      {/* Filter & Search Bar */}
      <div className="p-4 rounded-lg bg-white border border-zinc-200 shadow-xs flex flex-col md:flex-row gap-4 justify-between items-center text-xs">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-zinc-400 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Cerca per cliente, ID o descrizione..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="shadcn-input pl-9 w-full text-xs"
          />
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto justify-end">
          <div className="flex items-center gap-1.5 text-zinc-500">
            <Filter className="w-3.5 h-3.5" />
            <span>Filtra:</span>
          </div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="shadcn-input text-xs cursor-pointer"
          >
            <option value="ALL">Tutti gli stati</option>
            <option value="SUCCEEDED">Approvati</option>
            <option value="FAILED">Rifiutati</option>
            <option value="PARTIALLY_REFUNDED">Parz. Rimborsati</option>
            <option value="REFUNDED">Rimborsati Totali</option>
          </select>
        </div>
      </div>

      {/* Transactions Table (Shadcn Light style) */}
      <div className="rounded-lg border border-zinc-200 overflow-hidden bg-white shadow-xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-zinc-200 text-[11px] font-semibold text-zinc-500 uppercase tracking-wider bg-zinc-50">
                <th className="py-3 px-4">ID Transazione</th>
                <th className="py-3 px-4">Cliente & Descrizione</th>
                <th className="py-3 px-4">Importo</th>
                <th className="py-3 px-4">Stato</th>
                <th className="py-3 px-4">Data</th>
                <th className="py-3 px-4 text-right">Azioni</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100 text-xs">
              {loading ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-zinc-400">
                    Caricamento transazioni...
                  </td>
                </tr>
              ) : filteredPayments.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-zinc-400">
                    Nessuna transazione trovata.
                  </td>
                </tr>
              ) : (
                filteredPayments.map((p) => (
                  <tr key={p.id} className="hover:bg-zinc-50 transition-colors">
                    <td className="py-3.5 px-4 font-mono font-medium text-zinc-900">{p.id}</td>
                    <td className="py-3.5 px-4">
                      <div className="font-medium text-zinc-900">{p.description || 'Pagamento online'}</div>
                      <div className="text-[11px] text-zinc-500">{p.customerEmail || 'cliente@mail.com'}</div>
                    </td>
                    <td className="py-3.5 px-4 font-mono font-bold text-zinc-900">
                      {formatCurrency(p.amountCents)}
                      {p.refundedAmountCents && p.refundedAmountCents > 0 ? (
                        <div className="text-[10px] text-rose-600 font-normal">
                          - {formatCurrency(p.refundedAmountCents)}
                        </div>
                      ) : null}
                    </td>
                    <td className="py-3.5 px-4">
                      <span className={`text-[10px] font-mono font-semibold px-2 py-0.5 rounded border inline-flex items-center gap-1 ${
                        p.status === 'SUCCEEDED' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' :
                        p.status === 'FAILED' ? 'bg-rose-50 text-rose-700 border-rose-200' :
                        'bg-amber-50 text-amber-700 border-amber-200'
                      }`}>
                        {p.status === 'SUCCEEDED' ? 'APPROVATO' :
                         p.status === 'FAILED' ? 'RIFIUTATO' :
                         p.status === 'PARTIALLY_REFUNDED' ? 'PARZ. RIMBORSATO' : 'RIMBORSATO'}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-zinc-500">{formatDate(p.createdAt)}</td>
                    <td className="py-3.5 px-4 text-right space-x-2">
                      <button
                        onClick={() => setSelectedTx(p)}
                        className="p-1.5 rounded bg-zinc-100 hover:bg-zinc-200 text-zinc-700 transition-colors"
                        title="Vedi dettagli"
                      >
                        <Eye className="w-3.5 h-3.5" />
                      </button>

                      {(p.status === 'SUCCEEDED' || p.status === 'PARTIALLY_REFUNDED') && (
                        <button
                          onClick={() => openRefundModal(p)}
                          className="px-2.5 py-1 rounded bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 text-xs font-medium transition-colors inline-flex items-center gap-1"
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
        <div className="fixed inset-0 z-50 flex justify-end bg-black/40 backdrop-blur-xs animate-fadeIn">
          <div className="w-full max-w-md bg-white border-l border-zinc-200 p-6 flex flex-col justify-between h-full space-y-6">
            <div className="space-y-6 overflow-y-auto">
              <div className="flex items-center justify-between pb-3 border-b border-zinc-200">
                <div>
                  <h2 className="text-base font-bold text-zinc-900">Dettaglio Transazione</h2>
                  <p className="font-mono text-xs text-zinc-500">{selectedTx.id}</p>
                </div>
                <button onClick={() => setSelectedTx(null)} className="text-zinc-400 hover:text-zinc-900">
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="space-y-3 text-xs">
                <div className="p-4 rounded-lg bg-zinc-50 border border-zinc-200 space-y-2">
                  <div className="flex justify-between text-zinc-600">
                    <span>Importo Totale:</span>
                    <span className="font-mono font-bold text-zinc-900">{formatCurrency(selectedTx.amountCents)}</span>
                  </div>
                  <div className="flex justify-between text-zinc-600">
                    <span>Email Cliente:</span>
                    <span className="text-zinc-900 font-medium">{selectedTx.customerEmail || 'cliente@mail.com'}</span>
                  </div>
                  <div className="flex justify-between text-zinc-600">
                    <span>Codice Autorizzazione:</span>
                    <span className="font-mono text-zinc-900">{selectedTx.authorizationCode || 'N/D'}</span>
                  </div>
                </div>

                <h3 className="font-semibold text-zinc-900 pt-2">Timeline Ciclo di Vita</h3>
                <div className="space-y-3">
                  <div className="flex gap-3 text-xs">
                    <div className="w-2 h-2 rounded-full bg-zinc-400 mt-1.5" />
                    <div>
                      <div className="font-medium text-zinc-900">PaymentIntent Creato</div>
                      <div className="text-[10px] text-zinc-500">{formatDate(selectedTx.createdAt)}</div>
                    </div>
                  </div>
                  <div className="flex gap-3 text-xs">
                    <div className={`w-2 h-2 rounded-full ${selectedTx.status === 'FAILED' ? 'bg-rose-500' : 'bg-emerald-500'} mt-1.5`} />
                    <div>
                      <div className="font-medium text-zinc-900">
                        {selectedTx.status === 'FAILED' ? 'Autorizzazione Rifiutata' : 'Autorizzazione Completata'}
                      </div>
                      <div className="text-[10px] text-zinc-500">{formatDate(selectedTx.updatedAt)}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <button onClick={() => setSelectedTx(null)} className="btn-shadcn-secondary w-full text-xs py-2">
              Chiudi
            </button>
          </div>
        </div>
      )}

      {/* Refund Modal */}
      {refundModalTx && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs animate-fadeIn">
          <div className="p-6 rounded-lg bg-white border border-zinc-200 max-w-md w-full space-y-5 shadow-lg">
            <div className="flex items-center justify-between pb-3 border-b border-zinc-200">
              <h2 className="text-base font-bold text-zinc-900">Esegui Rimborso</h2>
              <button onClick={() => setRefundModalTx(null)} className="text-zinc-400 hover:text-zinc-900">
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleRefundSubmit} className="space-y-4 text-xs">
              <div className="p-3 rounded bg-zinc-50 border border-zinc-200 space-y-1">
                <div className="text-zinc-600">ID: <span className="font-mono text-zinc-900">{refundModalTx.id}</span></div>
                <div className="text-zinc-600">Importo Originale: <span className="font-mono text-zinc-900">{formatCurrency(refundModalTx.amountCents)}</span></div>
              </div>

              <div>
                <label className="block text-zinc-700 font-medium mb-1">Importo da Rimborsare (€)</label>
                <input
                  type="number"
                  step="0.01"
                  value={refundAmount}
                  onChange={(e) => setRefundAmount(e.target.value)}
                  className="shadcn-input w-full font-semibold"
                  required
                />
              </div>

              <div>
                <label className="block text-zinc-700 font-medium mb-1">Motivo Rimborso</label>
                <input
                  type="text"
                  value={refundReason}
                  onChange={(e) => setRefundReason(e.target.value)}
                  className="shadcn-input w-full"
                  required
                />
              </div>

              <div className="pt-2 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setRefundModalTx(null)}
                  className="btn-shadcn-secondary text-xs px-3 py-1.5"
                >
                  Annulla
                </button>
                <button
                  type="submit"
                  disabled={isRefunding}
                  className="btn-shadcn-primary text-xs px-3 py-1.5"
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
