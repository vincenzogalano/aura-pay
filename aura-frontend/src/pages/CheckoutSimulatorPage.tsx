import React, { useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { paymentApi } from '../api/paymentApi';
import { invoiceApi } from '../api/invoiceApi';
import { vaultApi } from '../api/vaultApi';
import type { PaymentIntent } from '../types';
import { 
  CreditCard as CreditCardIcon, 
  ShieldCheck, 
  CheckCircle2, 
  FileText, 
  Webhook, 
  ArrowRight,
  Code2,
  Copy,
  Check,
  ShoppingBag,
  ChevronDown,
  ChevronUp
} from 'lucide-react';
import { toast } from 'sonner';

interface PresetCard {
  label: string;
  pan: string;
  amount: number;
  expectedResult: string;
}

export const CheckoutSimulatorPage: React.FC = () => {
  const { merchant, isTest } = useMerchant();

  const [cardHolder, setCardHolder] = useState<string>('Mario Rossi');
  const [cardNumber, setCardNumber] = useState<string>('4242 4242 4242 4242');
  const [expiry, setExpiry] = useState<string>('12/28');
  const [cvv, setCvv] = useState<string>('123');
  const [amount, setAmount] = useState<string>('125.00');
  const [description, setDescription] = useState<string>('Abbonamento SaaS Enterprise');
  const [customerEmail, setCustomerEmail] = useState<string>('mario.rossi@azienda.it');

  const [step, setStep] = useState<number>(0);
  const [processing, setProcessing] = useState<boolean>(false);
  const [activeToken, setActiveToken] = useState<string | null>(null);
  const [completedPayment, setCompletedPayment] = useState<PaymentIntent | null>(null);
  const [invoiceUrl, setInvoiceUrl] = useState<string | null>(null);
  const [copiedPayload, setCopiedPayload] = useState<boolean>(false);
  const [showDevInspector, setShowDevInspector] = useState<boolean>(false);

  const presetCards: PresetCard[] = [
    { label: 'Carta Valida (Successo)', pan: '4242••••4242', amount: 125.00, expectedResult: 'Autorizzato (00)' },
    { label: 'Fondi Insufficienti', pan: '4532••••1099', amount: 10.99, expectedResult: 'Errore 51' },
    { label: 'Carta Scaduta', pan: '4532••••1098', amount: 10.98, expectedResult: 'Errore 54' },
    { label: 'Sospetto Frode', pan: '4532••••1097', amount: 10.97, expectedResult: 'Errore 59' },
  ];

  const selectPreset = (preset: PresetCard) => {
    setAmount(preset.amount.toString());
    setCardNumber(preset.pan.includes('99') ? '4532 0000 0000 1099' : preset.pan.includes('98') ? '4532 0000 0000 1098' : preset.pan.includes('97') ? '4532 0000 0000 1097' : '4242 4242 4242 4242');
    toast.info(`Preset applicato: ${preset.label}`);
  };

  const handleSimulatePayment = async (e: React.FormEvent) => {
    e.preventDefault();
    setProcessing(true);
    setStep(1);
    setActiveToken(null);
    setCompletedPayment(null);
    setInvoiceUrl(null);

    const amountInCents = Math.round(parseFloat(amount) * 100);

    try {
      // 1. Tokenizzazione reale della carta sul microservizio Vault Service (Backend)
      const parts = expiry.split('/');
      const month = parseInt(parts[0] || '12', 10);
      let year = parseInt(parts[1] || '2028', 10);
      if (year < 100) year += 2000;

      let realToken = '';
      const tokenRes = await vaultApi.tokenize({
        cardNumber: cardNumber.replace(/\s+/g, ''),
        cardholderName: cardHolder,
        expirationMonth: month,
        expirationYear: year,
        cvv: cvv || '123',
      });
      realToken = tokenRes.token;

      setActiveToken(realToken);
      setStep(2);

      // 2. Creazione PaymentIntent sul microservizio Payment Orchestrator (Backend)
      const payment = await paymentApi.createPaymentIntent({
        merchantId: merchant.id,
        amountCents: amountInCents,
        currency: 'EUR',
        description,
        customerEmail,
        isTest,
      });

      // 3. Conferma ed elaborazione transazione tramite Bank Simulator & Vault (Backend)
      let finalPayment = payment;
      try {
        finalPayment = await paymentApi.confirmPayment(payment.id, {
          paymentMethodToken: realToken
        });
      } catch (err) {
        finalPayment = { ...payment, status: 'FAILED', failureReason: 'Autorizzazione bancaria rifiutata' };
      }

      setCompletedPayment(finalPayment);

      if (finalPayment.status === 'FAILED') {
        setStep(5);
        toast.error(`Transazione rifiutata: ${finalPayment.failureReason || 'Errore circuiti'}`);
        return;
      }

      setStep(3);
      await new Promise(r => setTimeout(r, 500));
      setStep(4);
      // La fattura viene generata in modo asincrono dal microservizio invoice-service
      // tramite l'evento Kafka PaymentSucceededEvent. Non è disponibile immediatamente.
      // L'utente può consultarla nella sezione Fatture dopo qualche secondo.
      setInvoiceUrl(null);
      setStep(5);

      toast.success('Pagamento autorizzato con successo dal Backend!');
    } catch (err) {
      toast.error('Errore durante l\'elaborazione della transazione');
    } finally {
      setProcessing(false);
    }
  };

  const currentPayloadJson = completedPayment ? JSON.stringify({
    eventId: `evt_${completedPayment.id}`,
    eventType: completedPayment.status === 'SUCCEEDED' ? 'aura.payment.succeeded.v1' : 'aura.payment.failed.v1',
    occurredAt: new Date().toISOString(),
    paymentIntentId: completedPayment.id,
    merchantId: merchant.id,
    amountCents: completedPayment.amountCents,
    currency: completedPayment.currency,
    status: completedPayment.status,
    authorizationCode: completedPayment.authorizationCode || null,
    isTest,
  }, null, 2) : '';

  const copyJsonPayload = () => {
    navigator.clipboard.writeText(currentPayloadJson);
    setCopiedPayload(true);
    toast.success('Payload copiato!');
    setTimeout(() => setCopiedPayload(false), 2000);
  };

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Title */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-zinc-800 pb-5">
        <div>
          <h1 className="text-xl font-bold text-zinc-100 tracking-tight">
            Checkout Demo
          </h1>
          <p className="text-zinc-400 text-xs mt-0.5">
            Simula un acquisto e verifica l'orchestrazione automatica dei microservizi.
          </p>
        </div>
      </div>

      {/* Preset Test Cards Selection */}
      <div className="p-4 rounded-lg bg-zinc-900/60 border border-zinc-800 space-y-3 text-xs">
        <div className="font-semibold text-zinc-300">Carte di Test Rapide</div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {presetCards.map((preset, idx) => (
            <button
              key={idx}
              type="button"
              onClick={() => selectPreset(preset)}
              className="p-3 rounded bg-zinc-950 border border-zinc-800 text-left hover:border-zinc-700 transition-colors"
            >
              <div className="font-medium text-zinc-200">{preset.label}</div>
              <div className="text-[11px] text-zinc-500 mt-0.5">€ {preset.amount.toFixed(2)} — {preset.expectedResult}</div>
            </button>
          ))}
        </div>
      </div>

      {/* Main Checkout Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: Order Summary */}
        <div className="p-5 rounded-lg bg-zinc-900/60 border border-zinc-800 space-y-5 flex flex-col justify-between">
          <div className="space-y-4 text-xs">
            <div className="flex items-center gap-2 pb-3 border-b border-zinc-800 text-zinc-100 font-bold text-sm">
              <ShoppingBag className="w-4 h-4 text-zinc-400" />
              <span>Riepilogo Ordine</span>
            </div>

            <div className="space-y-2">
              <div className="flex justify-between text-zinc-400">
                <span>Esercente:</span>
                <span className="font-medium text-zinc-200">{merchant.businessName}</span>
              </div>
              <div className="flex justify-between text-zinc-400">
                <span>Prodotto:</span>
                <span className="font-medium text-zinc-200">{description}</span>
              </div>
              <div className="pt-3 border-t border-zinc-800 flex justify-between items-center">
                <span className="font-bold text-zinc-300">Totale:</span>
                <span className="text-xl font-bold text-zinc-50 font-mono">€ {parseFloat(amount || '0').toFixed(2)}</span>
              </div>
            </div>
          </div>

          <div className="p-3 rounded bg-zinc-950 border border-zinc-800 text-[11px] text-zinc-500 flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-zinc-400 shrink-0" />
            <span>Cifratura SSL/TLS & Vault Security</span>
          </div>
        </div>

        {/* Right: Payment Form */}
        <div className="p-5 rounded-lg bg-zinc-900/60 border border-zinc-800 lg:col-span-2 space-y-5">
          <div className="flex items-center justify-between pb-3 border-b border-zinc-800 text-xs">
            <div className="flex items-center gap-2 font-bold text-zinc-100">
              <CreditCardIcon className="w-4 h-4 text-zinc-400" />
              <span>Dettagli Carta di Credito</span>
            </div>
          </div>

          <form onSubmit={handleSimulatePayment} className="space-y-4 text-xs">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-zinc-300 font-medium mb-1">Importo (€)</label>
                <input
                  type="number"
                  step="0.01"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="shadcn-input w-full font-bold text-sm"
                  required
                />
              </div>

              <div>
                <label className="block text-zinc-300 font-medium mb-1">Email Cliente</label>
                <input
                  type="email"
                  value={customerEmail}
                  onChange={(e) => setCustomerEmail(e.target.value)}
                  className="shadcn-input w-full"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-zinc-300 font-medium mb-1">Descrizione Pagamento</label>
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="shadcn-input w-full"
                required
              />
            </div>

            <div>
              <label className="block text-zinc-300 font-medium mb-1">Titolare Carta</label>
              <input
                type="text"
                value={cardHolder}
                onChange={(e) => setCardHolder(e.target.value)}
                className="shadcn-input w-full"
                required
              />
            </div>

            <div>
              <label className="block text-zinc-300 font-medium mb-1">Numero Carta (PAN)</label>
              <input
                type="text"
                value={cardNumber}
                onChange={(e) => setCardNumber(e.target.value)}
                className="shadcn-input w-full font-mono"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-zinc-300 font-medium mb-1">Scadenza</label>
                <input
                  type="text"
                  value={expiry}
                  onChange={(e) => setExpiry(e.target.value)}
                  placeholder="MM/YY"
                  className="shadcn-input w-full text-center font-mono"
                  required
                />
              </div>
              <div>
                <label className="block text-zinc-300 font-medium mb-1">CVC / CVV</label>
                <input
                  type="text"
                  maxLength={4}
                  value={cvv}
                  onChange={(e) => setCvv(e.target.value)}
                  className="shadcn-input w-full text-center font-mono"
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={processing}
              className="btn-shadcn-primary w-full text-xs font-semibold py-2.5 mt-2 flex items-center justify-center gap-2"
            >
              <span>{processing ? 'Elaborazione in corso...' : `Paga Ora (€ ${parseFloat(amount || '0').toFixed(2)})`}</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </form>

          {/* Stepline Visualizer */}
          {step > 0 && (
            <div className="pt-4 space-y-2 border-t border-zinc-800 text-xs">
              <h3 className="font-semibold text-zinc-200">Orchestrazione Microservizi</h3>
              
              <div className="space-y-1.5 text-xs">
                <div className={`p-2.5 rounded border flex items-center justify-between ${step >= 1 ? 'bg-zinc-900 border-zinc-700 text-zinc-200' : 'bg-zinc-950 border-zinc-800 text-zinc-600'}`}>
                  <span>1. Tokenizzazione Carta (`aura-vault-service`)</span>
                  {activeToken && <span className="font-mono text-[10px] text-zinc-400">{activeToken}</span>}
                </div>

                <div className={`p-2.5 rounded border flex items-center justify-between ${step >= 2 ? 'bg-zinc-900 border-zinc-700 text-zinc-200' : 'bg-zinc-950 border-zinc-800 text-zinc-600'}`}>
                  <span>2. Autorizzazione Bancaria (`aura-payment-orchestrator`)</span>
                  {completedPayment && (
                    <span className={`font-mono text-[10px] font-semibold px-2 py-0.5 rounded border ${completedPayment.status === 'SUCCEEDED' ? 'bg-emerald-950 text-emerald-400 border-emerald-800' : 'bg-rose-950 text-rose-400 border-rose-800'}`}>
                      {completedPayment.status === 'SUCCEEDED' ? 'APPROVATO' : 'RIFIUTATO'}
                    </span>
                  )}
                </div>

                <div className={`p-2.5 rounded border flex items-center justify-between ${step >= 3 ? 'bg-zinc-900 border-zinc-700 text-zinc-200' : 'bg-zinc-950 border-zinc-800 text-zinc-600'}`}>
                  <span>3. Registrazione Ledger (`aura-ledger-service`)</span>
                  {step >= 3 && <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />}
                </div>

                <div className={`p-2.5 rounded border flex items-center justify-between ${step >= 4 ? 'bg-zinc-900 border-zinc-700 text-zinc-200' : 'bg-zinc-950 border-zinc-800 text-zinc-600'}`}>
                  <span>4. Fattura PDF MinIO S3 (`aura-invoice-service`)</span>
                  {step >= 4 && (
                    invoiceUrl ? (
                      <a href={invoiceUrl} target="_blank" rel="noreferrer" className="text-zinc-300 hover:underline flex items-center gap-1">
                        <FileText className="w-3.5 h-3.5" />
                        <span>Scarica PDF</span>
                      </a>
                    ) : (
                      <a href="/invoices" className="text-zinc-400 hover:text-zinc-200 flex items-center gap-1 text-[10px]">
                        <FileText className="w-3.5 h-3.5" />
                        <span>Elaborazione asincrona — vai alle Fatture</span>
                      </a>
                    )
                  )}
                </div>

                <div className={`p-2.5 rounded border flex items-center justify-between ${step >= 5 ? 'bg-zinc-900 border-zinc-700 text-zinc-200' : 'bg-zinc-950 border-zinc-800 text-zinc-600'}`}>
                  <span>5. Notifica Webhook HMAC (`aura-webhook-service`)</span>
                  {step >= 5 && <Webhook className="w-3.5 h-3.5 text-emerald-400" />}
                </div>
              </div>

              {completedPayment && (
                <div className="pt-2">
                  <button
                    onClick={() => setShowDevInspector(!showDevInspector)}
                    className="text-xs text-zinc-400 hover:text-white flex items-center gap-1"
                  >
                    <Code2 className="w-3.5 h-3.5" />
                    <span>{showDevInspector ? 'Nascondi Payload Kafka' : 'Mostra Payload Kafka (Dev)'}</span>
                    {showDevInspector ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                  </button>

                  {showDevInspector && (
                    <div className="mt-2 relative">
                      <button onClick={copyJsonPayload} className="absolute right-3 top-3 text-zinc-400 hover:text-white flex items-center gap-1 text-[10px]">
                        {copiedPayload ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                        <span>{copiedPayload ? 'Copiato!' : 'Copia'}</span>
                      </button>
                      <pre className="p-3 rounded bg-zinc-950 border border-zinc-800 font-mono text-[11px] text-zinc-300 overflow-x-auto">
                        {currentPayloadJson}
                      </pre>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
