import React, { useEffect, useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { invoiceApi } from '../api/invoiceApi';
import type { Invoice } from '../types';
import { FileText, Download } from 'lucide-react';
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
      toast.success('Fattura PDF aperta per il download');
      window.open(res.downloadUrl, '_blank');
    } catch (err) {
      toast.error('Impossibile scaricare la fattura');
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Title */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-zinc-800 pb-5">
        <div>
          <h1 className="text-xl font-bold text-zinc-100 tracking-tight">
            Fatture e Documenti Fiscali PDF
          </h1>
          <p className="text-zinc-400 text-xs mt-0.5">
            Scarica i documenti contabili generati automaticamente in S3 MinIO.
          </p>
        </div>
      </div>

      {/* Invoices Table */}
      <div className="rounded-lg border border-zinc-800 overflow-hidden bg-zinc-950">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="border-b border-zinc-800 text-[11px] font-semibold text-zinc-400 uppercase tracking-wider bg-zinc-900/60">
                <th className="py-3 px-4">Numero Documento</th>
                <th className="py-3 px-4">Tipo</th>
                <th className="py-3 px-4">Importo</th>
                <th className="py-3 px-4">Modalità</th>
                <th className="py-3 px-4">Data Emissione</th>
                <th className="py-3 px-4 text-right">Azione</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/60">
              {loading ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-zinc-500">
                    Caricamento fatture...
                  </td>
                </tr>
              ) : invoices.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-zinc-500">
                    Nessuna fattura emessa.
                  </td>
                </tr>
              ) : (
                invoices.map((inv) => (
                  <tr key={inv.id} className="hover:bg-zinc-900/40 transition-colors">
                    <td className="py-3.5 px-4 font-mono font-medium text-zinc-200 flex items-center gap-2">
                      <FileText className="w-3.5 h-3.5 text-zinc-400" />
                      <span>{inv.invoiceNumber}</span>
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="text-[10px] font-mono font-semibold px-2 py-0.5 rounded border bg-zinc-900 border-zinc-800 text-zinc-300">
                        {inv.invoiceType === 'INVOICE' ? 'FATTURA' : 'NOTA DI CREDITO'}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 font-mono font-semibold text-zinc-100">
                      € {(inv.amountCents / 100).toFixed(2)}
                    </td>
                    <td className="py-3.5 px-4">
                      {inv.isTest ? (
                        <span className="text-[10px] text-amber-400 font-mono font-semibold">
                          SANDBOX TEST
                        </span>
                      ) : (
                        <span className="text-[10px] text-emerald-400 font-mono font-semibold">
                          PRODUZIONE LIVE
                        </span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 text-zinc-400">
                      {new Date(inv.createdAt).toLocaleDateString('it-IT')}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <button
                        onClick={() => handleDownloadPdf(inv)}
                        disabled={downloadingId === inv.id}
                        className="btn-shadcn-secondary text-xs px-3 py-1 inline-flex items-center gap-1.5"
                      >
                        <Download className="w-3.5 h-3.5" />
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
