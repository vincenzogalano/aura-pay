import { apiClient, handleApiErrorWithFallback } from './client';
import type { Invoice } from '../types';
import { mockInvoices } from './mockData';

const createMockInvoicePdfBlobUrl = (invoiceId: string) => {
  const htmlContent = `
    <!DOCTYPE html>
    <html lang="it">
    <head>
      <meta charset="UTF-8">
      <title>Fattura Fiscale ${invoiceId.toUpperCase()} - AuraPay</title>
      <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 40px; color: #0f172a; background: #fff; max-width: 800px; margin: 0 auto; }
        .header { display: flex; justify-content: space-between; border-bottom: 2px solid #4338ca; padding-bottom: 20px; }
        .logo { font-size: 26px; font-weight: 800; color: #4338ca; letter-spacing: -0.5px; }
        .watermark { position: fixed; top: 35%; left: 20%; font-size: 70px; color: rgba(239, 68, 68, 0.12); transform: rotate(-25deg); font-weight: 900; pointer-events: none; }
        .details-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 30px; line-height: 1.6; font-size: 14px; }
        .box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px; }
        .table { width: 100%; border-collapse: collapse; margin-top: 30px; font-size: 13px; }
        .table th, .table td { padding: 12px 16px; border-bottom: 1px solid #e2e8f0; text-align: left; }
        .table th { background: #f1f5f9; color: #475569; font-weight: 700; text-transform: uppercase; font-size: 11px; tracking-wide; }
        .total-box { margin-top: 30px; text-align: right; font-size: 18px; font-weight: 800; color: #0f172a; padding: 16px; background: #e0e7ff; border-radius: 12px; }
        .footer { margin-top: 50px; font-size: 11px; color: #94a3b8; border-top: 1px solid #e2e8f0; padding-top: 20px; text-align: center; }
        @media print { .no-print { display: none; } }
      </style>
    </head>
    <body>
      <div class="watermark">DOCUMENTO DI TEST (SANDBOX)</div>
      <div class="header">
        <div>
          <div class="logo">⚡ AuraPay Gateway</div>
          <p style="margin:4px 0; color:#64748b; font-size:12px;">Payment Infrastructure S.p.A.</p>
          <p style="margin:0; color:#64748b; font-size:11px;">Via Monte Napoleone 8, 20121 Milano (MI)</p>
        </div>
        <div style="text-align:right;">
          <h2 style="margin:0; color:#1e293b; font-size:20px;">FATTURA FISCALE</h2>
          <p style="margin:4px 0; color:#4338ca; font-size:14px; font-weight:bold;">#${invoiceId.toUpperCase()}</p>
          <p style="margin:0; color:#64748b; font-size:12px;">Data Emissione: ${new Date().toLocaleDateString('it-IT')}</p>
        </div>
      </div>

      <div class="details-grid">
        <div class="box">
          <strong style="color:#475569; font-size:11px; text-transform:uppercase;">Dati Esercente (Merchant)</strong><br>
          <strong style="font-size:15px; color:#0f172a;">Acme Tech Solutions S.r.l.</strong><br>
          P.IVA / C.F.: IT12345678901<br>
          Email: amministrazione@acmetech.it
        </div>
        <div class="box">
          <strong style="color:#475569; font-size:11px; text-transform:uppercase;">Dati Pagamento</strong><br>
          Metodo: Carta di Credito (Stripe/Vault)<br>
          Stato Documento: <span style="color:#16a34a; font-weight:bold;">PAGATO (200 OK)</span><br>
          Presigned URL MinIO: S3 Storage Verified
        </div>
      </div>

      <table class="table">
        <thead>
          <tr>
            <th>Descrizione Servizio</th>
            <th style="text-align:center;">Quantità</th>
            <th style="text-align:right;">Prezzo Unitario</th>
            <th style="text-align:right;">Importo Totale</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>
              <strong>Abbonamento Digital SaaS Enterprise</strong><br>
              <span style="color:#64748b; font-size:11px;">Transazione autorizzata tramite AuraPay Gateway Orchestrator</span>
            </td>
            <td style="text-align:center;">1</td>
            <td style="text-align:right;">€ 125,00</td>
            <td style="text-align:right; font-weight:bold;">€ 125,00</td>
          </tr>
        </tbody>
      </table>

      <div class="total-box">
        Totale Incassato: € 125,00 EUR
      </div>

      <div style="margin-top:20px; text-align:center;" class="no-print">
        <button onclick="window.print()" style="background:#4338ca; color:#fff; border:none; padding:10px 20px; border-radius:8px; font-weight:bold; cursor:pointer;">
          Stampa / Salva in PDF
        </button>
      </div>

      <div class="footer">
        Documento fiscale simulato emesso dall'infrastruttura AuraPay. Archiviazione sicura MinIO S3 Bucket (aurapay-invoices).
      </div>
    </body>
    </html>
  `;
  const blob = new Blob([htmlContent], { type: 'text/html' });
  return URL.createObjectURL(blob);
};

export const invoiceApi = {
  // GET /v1/invoices?merchantId=...&isTest=...
  getInvoices: async (merchantId: string, isTest: boolean): Promise<Invoice[]> => {
    try {
      const response = await apiClient.get('/v1/invoices', {
        params: { merchantId, isTest }
      });
      return response.data;
    } catch (error) {
      return handleApiErrorWithFallback(error, mockInvoices.filter(i => i.isTest === isTest));
    }
  },

  // GET /v1/invoices/{id}/download-url
  getDownloadUrl: async (invoiceId: string): Promise<{ downloadUrl: string; expiresAt: string }> => {
    try {
      const response = await apiClient.get(`/v1/invoices/${invoiceId}/download-url`);
      return response.data;
    } catch (error) {
      return handleApiErrorWithFallback(error, {
        downloadUrl: createMockInvoicePdfBlobUrl(invoiceId),
        expiresAt: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
      });
    }
  },
};
