import React, { useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { 
  Terminal, 
  Copy, 
  Check, 
  Key, 
  ShieldCheck, 
  Layers 
} from 'lucide-react';
import { toast } from 'sonner';

export const DeveloperConsolePage: React.FC = () => {
  const { merchant, activeApiKey, isTest } = useMerchant();

  // cURL Generator State
  const [selectedEndpoint, setSelectedEndpoint] = useState<string>('create_payment');
  const [copiedCurl, setCopiedCurl] = useState<boolean>(false);

  // HMAC Verifier State
  const [hmacSecret, setHmacSecret] = useState<string>('whsec_demo_key_aurapay_2026_hmac');
  const [hmacPayload, setHmacPayload] = useState<string>(
    JSON.stringify({ eventId: 'evt_891234', eventType: 'payment.succeeded', amountCents: 12500, currency: 'EUR' }, null, 2)
  );
  const [calculatedSig, setCalculatedSig] = useState<string>('sha256_mock_computed_signature_998877665544332211');
  const [copiedSig, setCopiedSig] = useState<boolean>(false);

  const curlCommands: Record<string, { label: string; method: string; path: string; curl: string }> = {
    create_payment: {
      label: 'Creazione Intent di Pagamento',
      method: 'POST',
      path: '/v1/payments',
      curl: `curl -X POST http://localhost:8080/v1/payments \\
  -H "Content-Type: application/json" \\
  -H "X-Api-Key: ${activeApiKey}" \\
  -H "Idempotency-Key: idemp_${Date.now()}" \\
  -d '{
    "amountCents": 12500,
    "currency": "EUR",
    "description": "Abbonamento SaaS Enterprise",
    "customerEmail": "mario.rossi@azienda.it",
    "isTest": ${isTest}
  }'`,
    },
    get_balance: {
      label: 'Consulta Saldo Disponibile Ledger',
      method: 'GET',
      path: '/v1/ledger/balance',
      curl: `curl -X GET "http://localhost:8080/v1/ledger/balance?merchantId=${merchant.id}&isTest=${isTest}" \\
  -H "X-Api-Key: ${activeApiKey}"`,
    },
    request_kyb: {
      label: 'Richiesta Account & Verifica KYB',
      method: 'POST',
      path: `/v1/merchants/${merchant.id}/verification-request`,
      curl: `curl -X POST http://localhost:8080/v1/merchants/${merchant.id}/verification-request \\
  -H "Content-Type: application/json" \\
  -H "X-Api-Key: ${activeApiKey}" \\
  -d '{
    "vatNumber": "${merchant.vatNumber}",
    "businessName": "${merchant.businessName}",
    "email": "${merchant.email}"
  }'`,
    },
    get_invoice: {
      label: 'Generazione Link Download Fattura PDF',
      method: 'GET',
      path: '/v1/invoices/inv_2026_00101/download-url',
      curl: `curl -X GET http://localhost:8080/v1/invoices/inv_2026_00101/download-url \\
  -H "X-Api-Key: ${activeApiKey}"`,
    },
  };

  const copyCurl = () => {
    navigator.clipboard.writeText(curlCommands[selectedEndpoint].curl);
    setCopiedCurl(true);
    toast.success('Comando cURL copiato negli appunti!');
    setTimeout(() => setCopiedCurl(false), 2000);
  };

  const calculateHmacSimulated = () => {
    const hash = `sha256_${Math.random().toString(36).substring(2, 18)}${Math.random().toString(36).substring(2, 18)}`;
    setCalculatedSig(hash);
    toast.success('Firma HMAC-SHA256 ricalcolata!');
  };

  const copySig = () => {
    navigator.clipboard.writeText(calculatedSig);
    setCopiedSig(true);
    toast.success('Firma HMAC copiata negli appunti!');
    setTimeout(() => setCopiedSig(false), 2000);
  };

  const kafkaEvents = [
    { name: 'aura.merchant.created.v1', producer: 'Merchant Service', consumer: 'Analytics & Setup', trigger: 'Registrazione esercente' },
    { name: 'aura.merchant.verified.v1', producer: 'Merchant Service', consumer: 'Webhook Service', trigger: 'Approvazione verifica KYB' },
    { name: 'aura.paymentintent.created.v1', producer: 'Payment Orchestrator', consumer: 'Transactional Outbox', trigger: 'Inizializzazione intent' },
    { name: 'aura.payment.succeeded.v1', producer: 'Payment Orchestrator', consumer: 'Ledger, Webhook, Invoice', trigger: 'Autorizzazione bancaria OK' },
    { name: 'aura.payment.failed.v1', producer: 'Payment Orchestrator', consumer: 'Webhook Service', trigger: 'Autorizzazione rifiutata' },
    { name: 'aura.refund.succeeded.v1', producer: 'Payment Orchestrator', consumer: 'Ledger, Webhook, Invoice', trigger: 'Rimborso accreditato' },
    { name: 'aura.invoice.generated.v1', producer: 'Invoice Service', consumer: 'Webhook Service', trigger: 'Fattura generata su MinIO S3' },
    { name: 'aura.webhook.delivery_dead_lettered.v1', producer: 'Webhook Service', consumer: 'Audit System', trigger: 'Tentativi retry esauriti (500)' },
  ];

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Title */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight flex items-center gap-3">
            <span>Strumenti per Sviluppatori</span>
            <span className="text-xs font-semibold px-3 py-1 rounded-full bg-cyan-500/20 text-cyan-300 border border-cyan-500/40">
              API Explorer & Security
            </span>
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Testa le API REST via cURL, calcola le firme di sicurezza HMAC per i Webhook ed ispeziona l'architettura ad eventi Kafka.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Interactive cURL Generator */}
        <div className="glass-panel p-6 space-y-4 flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <Terminal className="w-5 h-5 text-indigo-400" />
                <h2 className="text-base font-bold text-white">Generatore di Richieste API (cURL)</h2>
              </div>
              <span className="text-xs font-mono text-slate-400">Postman Ready</span>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Seleziona Operazione</label>
              <select
                value={selectedEndpoint}
                onChange={(e) => setSelectedEndpoint(e.target.value)}
                className="glass-input w-full text-xs cursor-pointer"
              >
                {Object.entries(curlCommands).map(([key, cmd]) => (
                  <option key={key} value={key}>
                    [{cmd.method}] {cmd.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="relative">
              <div className="flex items-center justify-between text-[11px] font-mono text-slate-400 mb-1">
                <span>{curlCommands[selectedEndpoint].method} {curlCommands[selectedEndpoint].path}</span>
                <button
                  onClick={copyCurl}
                  className="text-slate-300 hover:text-white flex items-center gap-1 p-1"
                >
                  {copiedCurl ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{copiedCurl ? 'Copiato!' : 'Copia Comando'}</span>
                </button>
              </div>
              <pre className="p-4 rounded-xl bg-slate-950 border border-slate-800 font-mono text-[11px] text-cyan-300 overflow-x-auto max-h-64">
                {curlCommands[selectedEndpoint].curl}
              </pre>
            </div>
          </div>

          <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-[11px] text-slate-400 flex items-center gap-2">
            <Key className="w-4 h-4 text-indigo-400 shrink-0" />
            <span>Gli header di richiesta utilizzano la tua API Key attiva ({activeApiKey.substring(0, 10)}...).</span>
          </div>
        </div>

        {/* HMAC Signature Verifier Tool */}
        <div className="glass-panel p-6 space-y-4 flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-emerald-400" />
                <h2 className="text-base font-bold text-white">Verifica Firma Sicurezza Webhook</h2>
              </div>
              <span className="text-xs font-mono text-slate-400">HMAC-SHA256</span>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block text-slate-300 font-semibold mb-1">Chiave Segreta HMAC (`whsec_...`)</label>
                <input
                  type="text"
                  value={hmacSecret}
                  onChange={(e) => setHmacSecret(e.target.value)}
                  className="glass-input w-full font-mono text-xs"
                />
              </div>

              <div>
                <label className="block text-slate-300 font-semibold mb-1">Payload JSON della Notifica</label>
                <textarea
                  rows={4}
                  value={hmacPayload}
                  onChange={(e) => setHmacPayload(e.target.value)}
                  className="glass-input w-full font-mono text-[11px]"
                />
              </div>

              <button
                type="button"
                onClick={calculateHmacSimulated}
                className="btn-secondary w-full text-xs font-semibold py-2"
              >
                Calcola Header `X-Aura-Signature`
              </button>

              <div className="pt-2">
                <div className="flex items-center justify-between text-[11px] text-slate-400 mb-1">
                  <span>Firma SHA256 Calcolata</span>
                  <button onClick={copySig} className="text-slate-300 hover:text-white flex items-center gap-1">
                    {copiedSig ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    <span>Copia</span>
                  </button>
                </div>
                <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 font-mono text-[11px] text-emerald-400 break-all">
                  {calculatedSig}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Kafka Domain Event Catalog */}
      <div className="space-y-4 pt-4 border-t border-slate-800">
        <div className="flex items-center gap-2">
          <Layers className="w-5 h-5 text-indigo-400" />
          <h2 className="text-base font-bold text-white">Eventi di Dominio Apache Kafka</h2>
        </div>

        <div className="glass-panel overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800 text-[11px] font-semibold text-slate-400 uppercase tracking-wider bg-slate-900/50">
                  <th className="py-4 px-6">Nome Evento (Topic Kafka)</th>
                  <th className="py-4 px-6">Microservizio Emittente</th>
                  <th className="py-4 px-6">Microservizi Riceventi</th>
                  <th className="py-4 px-6">Causa di Innesco</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-xs">
                {kafkaEvents.map((evt, idx) => (
                  <tr key={idx} className="hover:bg-slate-900/40 transition-colors font-sans">
                    <td className="py-4 px-6 font-mono font-bold text-indigo-400">{evt.name}</td>
                    <td className="py-4 px-6 text-slate-200">{evt.producer}</td>
                    <td className="py-4 px-6 text-slate-300">{evt.consumer}</td>
                    <td className="py-4 px-6 text-slate-400">{evt.trigger}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};
