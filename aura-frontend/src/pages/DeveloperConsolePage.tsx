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

  const [selectedEndpoint, setSelectedEndpoint] = useState<string>('create_payment');
  const [copiedCurl, setCopiedCurl] = useState<boolean>(false);

  const [hmacSecret, setHmacSecret] = useState<string>('whsec_demo_key_aurapay_2026_hmac');
  const [hmacPayload, setHmacPayload] = useState<string>(
    JSON.stringify({ eventId: 'evt_891234', eventType: 'payment.succeeded', amountCents: 12500, currency: 'EUR' }, null, 2)
  );
  const [calculatedSig, setCalculatedSig] = useState<string>('sha256_mock_computed_signature_998877665544332211');
  const [copiedSig, setCopiedSig] = useState<boolean>(false);

  const curlCommands: Record<string, { label: string; method: string; path: string; curl: string }> = {
    create_payment: {
      label: 'Creazione Intent Pagamento',
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
      label: 'Richiesta Verifica KYB',
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
      label: 'Download Fattura PDF Presigned URL',
      method: 'GET',
      path: '/v1/invoices/inv_2026_00101/download-url',
      curl: `curl -X GET http://localhost:8080/v1/invoices/inv_2026_00101/download-url \\
  -H "X-Api-Key: ${activeApiKey}"`,
    },
  };

  const copyCurl = () => {
    navigator.clipboard.writeText(curlCommands[selectedEndpoint].curl);
    setCopiedCurl(true);
    toast.success('Comando cURL copiato!');
    setTimeout(() => setCopiedCurl(false), 2000);
  };

  const calculateHmacSimulated = () => {
    const hash = `sha256_${Math.random().toString(36).substring(2, 18)}${Math.random().toString(36).substring(2, 18)}`;
    setCalculatedSig(hash);
    toast.success('Firma HMAC ricalcolata!');
  };

  const copySig = () => {
    navigator.clipboard.writeText(calculatedSig);
    setCopiedSig(true);
    toast.success('Firma copiata!');
    setTimeout(() => setCopiedSig(false), 2000);
  };

  const kafkaEvents = [
    { name: 'aura.merchant.created.v1', producer: 'Merchant Service', consumer: 'Analytics', trigger: 'Registrazione merchant' },
    { name: 'aura.merchant.verified.v1', producer: 'Merchant Service', consumer: 'Webhook Service', trigger: 'Approvazione KYB' },
    { name: 'aura.paymentintent.created.v1', producer: 'Payment Orchestrator', consumer: 'Transactional Outbox', trigger: 'Creazione intent' },
    { name: 'aura.payment.succeeded.v1', producer: 'Payment Orchestrator', consumer: 'Ledger, Webhook, Invoice', trigger: 'Autorizzazione bancaria OK' },
    { name: 'aura.payment.failed.v1', producer: 'Payment Orchestrator', consumer: 'Webhook Service', trigger: 'Autorizzazione rifiutata' },
    { name: 'aura.refund.succeeded.v1', producer: 'Payment Orchestrator', consumer: 'Ledger, Webhook, Invoice', trigger: 'Rimborso completato' },
    { name: 'aura.invoice.generated.v1', producer: 'Invoice Service', consumer: 'Webhook Service', trigger: 'PDF generato su MinIO S3' },
  ];

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Title */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-zinc-800 pb-5">
        <div>
          <h1 className="text-xl font-bold text-zinc-100 tracking-tight">
            Strumenti Sviluppatori
          </h1>
          <p className="text-zinc-400 text-xs mt-0.5">
            Testa le API via cURL, calcola le firme di sicurezza HMAC e consulta gli eventi Kafka.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Interactive cURL Generator */}
        <div className="p-5 rounded-lg bg-zinc-900/60 border border-zinc-800 space-y-4 flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-zinc-800 text-xs font-semibold text-zinc-100">
              <div className="flex items-center gap-2">
                <Terminal className="w-4 h-4 text-zinc-400" />
                <span>Generatore Richieste cURL</span>
              </div>
            </div>

            <div>
              <label className="block text-xs text-zinc-400 mb-1">Seleziona Operazione API</label>
              <select
                value={selectedEndpoint}
                onChange={(e) => setSelectedEndpoint(e.target.value)}
                className="shadcn-input w-full text-xs cursor-pointer"
              >
                {Object.entries(curlCommands).map(([key, cmd]) => (
                  <option key={key} value={key}>
                    [{cmd.method}] {cmd.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="relative">
              <div className="flex items-center justify-between text-[11px] font-mono text-zinc-400 mb-1">
                <span>{curlCommands[selectedEndpoint].method} {curlCommands[selectedEndpoint].path}</span>
                <button onClick={copyCurl} className="text-zinc-300 hover:text-white flex items-center gap-1">
                  {copiedCurl ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                  <span>{copiedCurl ? 'Copiato!' : 'Copia'}</span>
                </button>
              </div>
              <pre className="p-3 rounded bg-zinc-950 border border-zinc-800 font-mono text-[11px] text-zinc-200 overflow-x-auto max-h-60">
                {curlCommands[selectedEndpoint].curl}
              </pre>
            </div>
          </div>

          <div className="p-2.5 rounded bg-zinc-950 border border-zinc-800 text-[11px] text-zinc-500 flex items-center gap-2">
            <Key className="w-3.5 h-3.5 text-zinc-400 shrink-0" />
            <span>Utilizza la tua API Key attiva: {activeApiKey.substring(0, 12)}...</span>
          </div>
        </div>

        {/* HMAC Signature Verifier Tool */}
        <div className="p-5 rounded-lg bg-zinc-900/60 border border-zinc-800 space-y-4 flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-zinc-800 text-xs font-semibold text-zinc-100">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-zinc-400" />
                <span>Verifica Firma HMAC Webhook</span>
              </div>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block text-zinc-400 font-medium mb-1">Secret Key HMAC</label>
                <input
                  type="text"
                  value={hmacSecret}
                  onChange={(e) => setHmacSecret(e.target.value)}
                  className="shadcn-input w-full font-mono text-xs"
                />
              </div>

              <div>
                <label className="block text-zinc-400 font-medium mb-1">Payload JSON Notifica</label>
                <textarea
                  rows={4}
                  value={hmacPayload}
                  onChange={(e) => setHmacPayload(e.target.value)}
                  className="shadcn-input w-full font-mono text-[11px]"
                />
              </div>

              <button
                type="button"
                onClick={calculateHmacSimulated}
                className="btn-shadcn-secondary w-full text-xs py-2"
              >
                Calcola Firma `X-Aura-Signature`
              </button>

              <div className="pt-2">
                <div className="flex items-center justify-between text-[11px] text-zinc-400 mb-1">
                  <span>Firma SHA256 Calcolata</span>
                  <button onClick={copySig} className="text-zinc-300 hover:text-white flex items-center gap-1">
                    {copiedSig ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                    <span>Copia</span>
                  </button>
                </div>
                <div className="p-2.5 rounded bg-zinc-950 border border-zinc-800 font-mono text-[11px] text-zinc-300 break-all">
                  {calculatedSig}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Kafka Domain Event Catalog */}
      <div className="space-y-3 pt-4 border-t border-zinc-800">
        <div className="flex items-center gap-2 text-sm font-semibold text-zinc-100">
          <Layers className="w-4 h-4 text-zinc-400" />
          <span>Catalogo Eventi Apache Kafka</span>
        </div>

        <div className="rounded-lg border border-zinc-800 overflow-hidden bg-zinc-950">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-zinc-800 text-[11px] font-semibold text-zinc-400 uppercase tracking-wider bg-zinc-900/60">
                  <th className="py-3 px-4">Nome Evento (Topic)</th>
                  <th className="py-3 px-4">Emittente</th>
                  <th className="py-3 px-4">Riceventi</th>
                  <th className="py-3 px-4">Innesco</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/60 font-mono">
                {kafkaEvents.map((evt, idx) => (
                  <tr key={idx} className="hover:bg-zinc-900/40 transition-colors">
                    <td className="py-3 px-4 font-semibold text-zinc-200">{evt.name}</td>
                    <td className="py-3 px-4 text-zinc-400 font-sans">{evt.producer}</td>
                    <td className="py-3 px-4 text-zinc-400 font-sans">{evt.consumer}</td>
                    <td className="py-3 px-4 text-zinc-400 font-sans">{evt.trigger}</td>
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
