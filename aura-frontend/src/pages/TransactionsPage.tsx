import React, { useEffect, useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { paymentApi } from '../api/paymentApi';
import type { PaymentIntent } from '../types';
import { 
  Search, 
  Filter, 
  RotateCcw, 
  Eye, 
  X, 
  CheckCircle2, 
  AlertCircle, 
  CreditCard
} from 'lucide-react';
import { toast } from 'sonner';

export const TransactionsPage: React.FC = () => {
  const { merchant, isTest } = useMerchant();
  const [payments, setPayments] = useState<PaymentIntent[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  
  // Filtri & Ricerca
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');

  // Modali State
  const [selectedPayment, setSelectedPayment] = useState<PaymentIntent | null>(null);
  const [refundPayment, setRefundPayment] = useState<PaymentIntent | null>(null);
  const [refundAmount, setRefundAmount] = useState<string>('');
  const [refundReason, setRefundReason] = useState<string>('');
  const [refunding, setRefunding] = useState<boolean>(false);

  const fetchPayments = async () => {
    setLoading(true);
    try {
      const data = await paymentApi.getPayments({ isTest });
      setPayments(data);
    } catch (err) {
      toast.error('Errore nel recupero transazioni');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPayments();
  }, [merchant.id, isTest]);

  // Filtraggio
  const filteredPayments = payments.filter((p) => {
    const matchesSearch = p.id.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          (p.customerEmail && p.customerEmail.toLowerCase().includes(searchQuery.toLowerCase())) ||
                          (p.description && p.description.toLowerCase().includes(searchQuery.toLowerCase()));
    const matchesStatus = selectedStatus === 'ALL' || p.status === selectedStatus;
    return matchesSearch && matchesStatus;
  });

  // Gestione Rimborso
  const handleExecuteRefund = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!refundPayment) return;

    const amountInCents = Math.round(parseFloat(refundAmount) * 100);
    const maxRefundable = refundPayment.amountCents - (refundPayment.refundedAmountCents || 0);

    if (isNaN(amountInCents) || amountInCents <= 0) {
      toast.error('Inserisci un importo di rimborso valido!');
      return;
    }

    if (amountInCents > maxRefundable) {
      toast.error(`L'importo non può superare il residuo rimborsabile di €${(maxRefundable / 100).toFixed(2)}`);
      return;
    }

    setRefunding(true);
    try {
      await paymentApi.refundPayment(refundPayment.id, {
        amountCents: amountInCents,
        reason: refundReason || 'Rimborso richiesto da dashboard',
      });
      toast.success(`Rimborso di €${(amountInCents / 100).toFixed(2)} eseguito con successo!`);
      setRefundPayment(null);
      setRefundAmount('');
      setRefundReason('');
      fetchPayments();
    } catch (err) {
      toast.error('Errore durante l\'esecuzione del rimborso');
    } finally {
      setRefunding(false);
    }
  };

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Page Title & Stats */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
            Transazioni & Rimborsi
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Gestisci ed ispeziona le autorizzazioni di pagamento in ambiente {isTest ? 'Sandbox' : 'Live'}.
          </p>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="glass-panel p-4 flex flex-col md:flex-row items-center justify-between gap-4">
        {/* Search */}
        <div className="relative w-full md:w-96">
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Cerca per PaymentID, email cliente o descrizione..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="glass-input pl-10 w-full text-xs"
          />
        </div>

        {/* Filter Dropdown */}
        <div className="flex items-center gap-3 w-full md:w-auto">
          <Filter className="w-4 h-4 text-slate-400" />
          <select
            value={selectedStatus}
            onChange={(e) => setSelectedStatus(e.target.value)}
            className="glass-input text-xs pr-8 cursor-pointer"
          >
            <option value="ALL">Tutti gli Stati</option>
            <option value="SUCCEEDED">SUCCEEDED</option>
            <option value="PROCESSING">PROCESSING</option>
            <option value="FAILED">FAILED</option>
            <option value="PARTIALLY_REFUNDED">PARTIALLY_REFUNDED</option>
            <option value="REFUNDED">REFUNDED</option>
          </select>
        </div>
      </div>

      {/* Payments Table */}
      <div className="glass-panel overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-[11px] font-semibold text-slate-400 uppercase tracking-wider bg-slate-900/50">
                <th className="py-4 px-6">ID Transazione</th>
                <th className="py-4 px-6">Cliente & Descrizione</th>
                <th className="py-4 px-6">Importo</th>
                <th className="py-4 px-6">Stato</th>
                <th className="py-4 px-6">Ambiente</th>
                <th className="py-4 px-6">Data</th>
                <th className="py-4 px-6 text-right">Azioni</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-xs">
              {loading ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-slate-500">
                    Caricamento transazioni in corso...
                  </td>
                </tr>
              ) : filteredPayments.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-slate-500">
                    Nessuna transazione trovata per i criteri selezionati.
                  </td>
                </tr>
              ) : (
                filteredPayments.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-900/40 transition-colors">
                    <td className="py-4 px-6 font-mono font-medium text-indigo-400">
                      {p.id}
                    </td>
                    <td className="py-4 px-6">
                      <div className="font-semibold text-slate-200">{p.description || 'Pagamento Direct API'}</div>
                      <div className="text-[11px] text-slate-400">{p.customerEmail || 'N/D'}</div>
                    </td>
                    <td className="py-4 px-6 font-bold text-white">
                      €{(p.amountCents / 100).toFixed(2)}
                      {p.refundedAmountCents ? (
                        <div className="text-[10px] text-purple-400 font-normal">
                          (Rimborsati €{(p.refundedAmountCents / 100).toFixed(2)})
                        </div>
                      ) : null}
                    </td>
                    <td className="py-4 px-6">
                      <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-bold ${
                        p.status === 'SUCCEEDED' ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' :
                        p.status === 'FAILED' ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30' :
                        p.status === 'PARTIALLY_REFUNDED' ? 'bg-purple-500/20 text-purple-400 border border-purple-500/30' :
                        p.status === 'REFUNDED' ? 'bg-slate-700/50 text-slate-300 border border-slate-600' :
                        'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                      }`}>
                        {p.status}
                      </span>
                    </td>
                    <td className="py-4 px-6">
                      {p.isTest ? (
                        <span className="text-[10px] text-amber-400 font-semibold bg-amber-500/10 border border-amber-500/20 px-2 py-0.5 rounded-full">SANDBOX</span>
                      ) : (
                        <span className="text-[10px] text-emerald-400 font-semibold bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded-full">LIVE</span>
                      )}
                    </td>
                    <td className="py-4 px-6 text-slate-400">
                      {new Date(p.createdAt).toLocaleDateString()} {new Date(p.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </td>
                    <td className="py-4 px-6 text-right space-x-2">
                      <button
                        onClick={() => setSelectedPayment(p)}
                        className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-colors"
                        title="Vedi Dettaglio"
                      >
                        <Eye className="w-4 h-4" />
                      </button>

                      {(p.status === 'SUCCEEDED' || p.status === 'PARTIALLY_REFUNDED') && (
                        <button
                          onClick={() => {
                            setRefundPayment(p);
                            const max = (p.amountCents - (p.refundedAmountCents || 0)) / 100;
                            setRefundAmount(max.toString());
                          }}
                          className="p-1.5 rounded-lg bg-purple-900/40 hover:bg-purple-900/60 text-purple-300 border border-purple-700/50 transition-colors"
                          title="Esegui Rimborso"
                        >
                          <RotateCcw className="w-4 h-4" />
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

      {/* Modal Dettaglio Transazione */}
      {selectedPayment && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 backdrop-blur-sm p-4">
          <div className="glass-panel w-full max-w-2xl p-6 space-y-6 animate-scaleIn relative">
            <button
              onClick={() => setSelectedPayment(null)}
              className="absolute top-4 right-4 text-slate-400 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-3">
              <div className="p-3 rounded-xl bg-indigo-600/20 border border-indigo-500/30 text-indigo-400">
                <CreditCard className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-xl font-bold text-white">Dettaglio PaymentIntent</h3>
                <p className="text-xs text-slate-400 font-mono">{selectedPayment.id}</p>
              </div>
            </div>

            {/* Content Details */}
            <div className="grid grid-cols-2 gap-4 text-xs bg-slate-950/60 p-4 rounded-xl border border-slate-800">
              <div>
                <span className="text-slate-400 block">Importo Totale</span>
                <span className="text-base font-bold text-white">€{(selectedPayment.amountCents / 100).toFixed(2)}</span>
              </div>
              <div>
                <span className="text-slate-400 block">Cliente</span>
                <span className="font-semibold text-slate-200">{selectedPayment.customerEmail || 'Non specificato'}</span>
              </div>
              <div>
                <span className="text-slate-400 block">Authorization Code</span>
                <span className="font-mono text-emerald-400">{selectedPayment.authorizationCode || 'N/D'}</span>
              </div>
              <div>
                <span className="text-slate-400 block">Bank Transaction ID</span>
                <span className="font-mono text-cyan-400">{selectedPayment.bankTransactionId || 'N/D'}</span>
              </div>
            </div>

            {/* Timeline Eventi */}
            <div className="space-y-3">
              <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider">Timeline del Ciclo di Vita</h4>
              <div className="space-y-2 text-xs">
                <div className="flex items-center gap-3 p-2.5 rounded-lg bg-slate-900/60 border border-slate-800">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                  <div>
                    <span className="font-semibold text-slate-200">CREATED</span>
                    <span className="text-slate-400 block text-[10px]">PaymentIntent creato con successo via API</span>
                  </div>
                </div>

                <div className="flex items-center gap-3 p-2.5 rounded-lg bg-slate-900/60 border border-slate-800">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                  <div>
                    <span className="font-semibold text-slate-200">PROCESSING</span>
                    <span className="text-slate-400 block text-[10px]">Detokenizzazione Vault & Autorizzazione Bank Simulator</span>
                  </div>
                </div>

                <div className="flex items-center gap-3 p-2.5 rounded-lg bg-slate-900/60 border border-slate-800">
                  {selectedPayment.status === 'SUCCEEDED' || selectedPayment.status === 'PARTIALLY_REFUNDED' || selectedPayment.status === 'REFUNDED' ? (
                    <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                  ) : (
                    <AlertCircle className="w-4 h-4 text-rose-400" />
                  )}
                  <div>
                    <span className="font-semibold text-slate-200">{selectedPayment.status}</span>
                    <span className="text-slate-400 block text-[10px]">
                      {selectedPayment.failureReason || 'Esito autorizzazione completato con esito positivo'}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div className="flex justify-end">
              <button
                onClick={() => setSelectedPayment(null)}
                className="btn-secondary text-xs"
              >
                Chiudi
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Esegui Rimborso */}
      {refundPayment && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 backdrop-blur-sm p-4">
          <div className="glass-panel w-full max-w-lg p-6 space-y-6 animate-scaleIn relative">
            <button
              onClick={() => setRefundPayment(null)}
              className="absolute top-4 right-4 text-slate-400 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-3">
              <div className="p-3 rounded-xl bg-purple-600/20 border border-purple-500/30 text-purple-400">
                <RotateCcw className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-xl font-bold text-white">Esegui Rimborso</h3>
                <p className="text-xs text-slate-400">Payment ID: {refundPayment.id}</p>
              </div>
            </div>

            <form onSubmit={handleExecuteRefund} className="space-y-4 text-xs">
              <div>
                <label className="block text-slate-300 font-semibold mb-1">
                  Importo da Rimborsare (€)
                </label>
                <input
                  type="number"
                  step="0.01"
                  max={(refundPayment.amountCents - (refundPayment.refundedAmountCents || 0)) / 100}
                  value={refundAmount}
                  onChange={(e) => setRefundAmount(e.target.value)}
                  className="glass-input w-full text-base font-bold"
                  required
                />
                <span className="text-[10px] text-slate-400 mt-1 block">
                  Massimo rimborsabile residuo: €{((refundPayment.amountCents - (refundPayment.refundedAmountCents || 0)) / 100).toFixed(2)}
                </span>
              </div>

              <div>
                <label className="block text-slate-300 font-semibold mb-1">Motivazione (opzionale)</label>
                <textarea
                  rows={2}
                  value={refundReason}
                  onChange={(e) => setRefundReason(e.target.value)}
                  placeholder="Es. Richiesta dal cliente per recesso entro 14gg"
                  className="glass-input w-full text-xs"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setRefundPayment(null)}
                  className="btn-secondary text-xs"
                >
                  Annulla
                </button>
                <button
                  type="submit"
                  disabled={refunding}
                  className="bg-purple-600 hover:bg-purple-500 text-white font-medium px-4 py-2.5 rounded-lg text-xs transition-colors shadow-lg shadow-purple-600/30"
                >
                  {refunding ? 'Elaborazione...' : 'Conferma Rimborso'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
