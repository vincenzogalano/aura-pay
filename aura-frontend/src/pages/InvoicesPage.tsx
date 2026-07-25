import React, { useEffect, useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { invoiceApi } from '../api/invoiceApi';
import type { Invoice } from '../types';
import { FileText, Download, CheckCircle2 } from 'lucide-react';
import { toast } from 'sonner';

export const InvoicesPage: React.FC = () => {
  const { merchant, isTest } = useMerchant();
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  useEffect(() => {
    const fetchInvoices = async () => {
      setLoading(true);
      try {
        const data = await invoiceApi.getInvoices(merchant.id, isTest);
        setInvoices(data);
      } catch (err) {
        toast.error('Errore nel recupero delle fatture');
      } finally {
        setLoading(false);
      }
    };
    fetchInvoices();
  }, [merchant.id, isTest]);

  const handleDownloadPdf = async (invoice: Invoice) => {
    setDownloadingId(invoice.id);
    try {
      const res = await invoiceApi.getDownloadUrl(invoice.id);
      toast.success(`Presigned URL generato! Scadenza: ${new Date(res.expiresAt).toLocaleTimeString()}`);
      
      // Apri link temporaneo in una nuova scheda
      window.open(res.downloadUrl, '_blank');
    } catch (err) {
      toast.error('Impossibile recuperare l\'URL firmata per il download');
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Title */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
            Fatture e Documenti Fiscali PDF
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Scarica i documenti contabili PDF generati da Aura-Invoice-Service tramite Presigned URL S3 (MinIO).
          </p>
        </div>
      </div>

      {/* Invoices Table */}
      <div className="glass-panel overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-[11px] font-semibold text-slate-400 uppercase tracking-wider bg-slate-900/50">
                <th className="py-4 px-6">Numero Documento</th>
                <th className="py-4 px-6">Tipo</th>
                <th className="py-4 px-6">Riferimento PaymentIntent</th>
                <th className="py-4 px-6">Importo</th>
                <th className="py-4 px-6">Watermark</th>
                <th className="py-4 px-6">Stato</th>
                <th className="py-4 px-6">Data Emissione</th>
                <th className="py-4 px-6 text-right">Azione</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-xs">
              {loading ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-500">
                    Caricamento fatture in corso...
                  </td>
                </tr>
              ) : invoices.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-500">
                    Nessuna fattura o nota di credito emessa in questo ambiente.
                  </td>
                </tr>
              ) : (
                invoices.map((inv) => (
                  <tr key={inv.id} className="hover:bg-slate-900/40 transition-colors">
                    <td className="py-4 px-6 font-mono font-bold text-white flex items-center gap-2">
                      <FileText className="w-4 h-4 text-indigo-400" />
                      <span>{inv.invoiceNumber}</span>
                    </td>
                    <td className="py-4 px-6">
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                        inv.invoiceType === 'INVOICE' 
                          ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30'
                          : 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                      }`}>
                        {inv.invoiceType}
                      </span>
                    </td>
                    <td className="py-4 px-6 font-mono text-slate-400">
                      {inv.paymentIntentId || inv.refundId || 'N/D'}
                    </td>
                    <td className="py-4 px-6 font-bold text-white">
                      €{(inv.amountCents / 100).toFixed(2)}
                    </td>
                    <td className="py-4 px-6">
                      {inv.isTest ? (
                        <span className="text-[10px] text-amber-400 font-semibold bg-amber-500/10 border border-amber-500/20 px-2 py-0.5 rounded-full">
                          TEST WATERMARK
                        </span>
                      ) : (
                        <span className="text-[10px] text-emerald-400 font-semibold bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded-full">
                          VALIDO FISCALMENTE
                        </span>
                      )}
                    </td>
                    <td className="py-4 px-6">
                      <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                        <CheckCircle2 className="w-3 h-3" />
                        <span>{inv.status}</span>
                      </span>
                    </td>
                    <td className="py-4 px-6 text-slate-400">
                      {new Date(inv.createdAt).toLocaleDateString()}
                    </td>
                    <td className="py-4 px-6 text-right">
                      <button
                        onClick={() => handleDownloadPdf(inv)}
                        disabled={downloadingId === inv.id}
                        className="btn-primary text-xs py-1.5 px-3 inline-flex items-center gap-1.5"
                      >
                        <Download className="w-3.5 h-3.5" />
                        <span>{downloadingId === inv.id ? 'Generazione URL...' : 'Scarica PDF'}</span>
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
