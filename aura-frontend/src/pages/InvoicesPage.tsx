import React, { useEffect, useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { invoiceApi } from '../api/invoiceApi';
import type { Invoice } from '../types';
import { FileText, Download } from 'lucide-react';
import { toast } from 'sonner';
import { getInvoiceStatusInfo } from '../utils/statusUtils';

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
      toast.success('Fattura PDF aperta per il download');
      window.open(res.downloadUrl, '_blank');
    } catch (err) {
      toast.error('Impossibile scaricare la fattura');
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <div className="space-y-6 max-w-5xl animate-fadeIn">
      {/* Title */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-zinc-200 pb-5">
        <div>
          <h1 className="text-xl font-bold text-zinc-900 tracking-tight">
            Fatture e Note di Credito PDF
          </h1>
          <p className="text-zinc-500 text-xs mt-0.5">
            Scarica i documenti contabili generati ed archiviati in modo sicuro.
          </p>
        </div>
      </div>

      {/* Invoices Table (High Contrast Light Theme) */}
      <div className="rounded-lg border border-zinc-200 overflow-hidden bg-white shadow-xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="border-b border-zinc-200 text-[11px] font-bold text-zinc-600 uppercase tracking-wider bg-zinc-50">
                <th className="py-3 px-4">Numero Documento</th>
                <th className="py-3 px-4">Tipo Documento</th>
                <th className="py-3 px-4">Importo Totale</th>
                <th className="py-3 px-4">Stato Emissione</th>
                <th className="py-3 px-4">Data Emissione</th>
                <th className="py-3 px-4 text-right">Azione</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100 font-mono">
              {loading ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-zinc-400 font-sans">
                    Caricamento documenti in corso...
                  </td>
                </tr>
              ) : invoices.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-zinc-400 font-sans">
                    Nessuna fattura o nota di credito emessa per questo esercente.
                  </td>
                </tr>
              ) : (
                (Array.isArray(invoices) ? invoices : []).map((inv, idx) => (
                  <tr key={inv.id ? `inv-${inv.id}` : `inv-idx-${idx}`} className="hover:bg-zinc-50 transition-colors">
                    <td className="py-3.5 px-4 font-mono font-bold text-zinc-900 flex items-center gap-2">
                      <FileText className="w-4 h-4 text-indigo-600" />
                      <span>{inv.invoiceNumber}</span>
                    </td>
                    <td className="py-3.5 px-4 font-sans font-medium">
                      <span className="text-[10px] font-semibold px-2 py-0.5 rounded border bg-zinc-100 border-zinc-200 text-zinc-800">
                        {inv.invoiceType === 'INVOICE' ? 'Fattura di Vendita' : 'Nota di Credito (Rimborso)'}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 font-mono font-bold text-zinc-900">
                      € {(inv.amountCents / 100).toFixed(2)}
                    </td>
                    <td className="py-3.5 px-4 font-sans">
                      {(() => {
                        const invInfo = getInvoiceStatusInfo(inv.status);
                        return (
                          <span 
                            title={invInfo.description}
                            className={`text-[10px] font-sans font-semibold px-2 py-0.5 rounded border inline-flex items-center gap-1 ${invInfo.bgClass} ${invInfo.textClass} ${invInfo.borderClass}`}
                          >
                            <span>{invInfo.icon}</span>
                            <span>{invInfo.label}</span>
                          </span>
                        );
                      })()}
                    </td>
                    <td className="py-3.5 px-4 text-zinc-500 font-sans">
                      {new Date(inv.createdAt).toLocaleDateString('it-IT')}
                    </td>
                    <td className="py-3.5 px-4 text-right font-sans">
                      <button
                        onClick={() => handleDownloadPdf(inv)}
                        disabled={downloadingId === inv.id}
                        className="btn-shadcn-secondary text-xs px-3 py-1 inline-flex items-center gap-1.5 font-medium hover:bg-zinc-100"
                      >
                        <Download className="w-3.5 h-3.5 text-indigo-600" />
                        <span>{downloadingId === inv.id ? 'Generazione...' : 'Scarica PDF'}</span>
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
