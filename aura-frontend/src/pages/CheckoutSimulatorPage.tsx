import React, { useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { paymentApi } from '../api/paymentApi';
import { invoiceApi } from '../api/invoiceApi';
import type { PaymentIntent } from '../types';
import { 
  CreditCard as CreditCardIcon, 
  ShieldCheck, 
  Sparkles, 
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
  badgeColor: string;
}

export const CheckoutSimulatorPage: React.FC = () => {
  const { merchant, isTest } = useMerchant();

  // Form State
  const [cardHolder, setCardHolder] = useState<string>('Mario Rossi');
  const [cardNumber, setCardNumber] = useState<string>('4242 4242 4242 4242');
  const [expiry, setExpiry] = useState<string>('12/28');
  const [cvv, setCvv] = useState<string>('123');
  const [amount, setAmount] = useState<string>('125.00');
  const [description, setDescription] = useState<string>('Abbonamento SaaS Enterprise (Piano Annuale)');
  const [customerEmail, setCustomerEmail] = useState<string>('mario.rossi@azienda.it');

  // Execution State
  const [step, setStep] = useState<number>(0);
  const [processing, setProcessing] = useState<boolean>(false);
  const [activeToken, setActiveToken] = useState<string | null>(null);
  const [completedPayment, setCompletedPayment] = useState<PaymentIntent | null>(null);
  const [invoiceUrl, setInvoiceUrl] = useState<string | null>(null);
  const [copiedPayload, setCopiedPayload] = useState<boolean>(false);
  const [showDevInspector, setShowDevInspector] = useState<boolean>(false);

  const presetCards: PresetCard[] = [
    { label: 'Carta Valida (Successo)', pan: '4242••••4242', amount: 125.00, expectedResult: 'Autorizzazione OK', badgeColor: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/40' },
    { label: 'Fondi Insufficienti', pan: '4532••••1099', amount: 10.99, expectedResult: 'Simula Errore 51', badgeColor: 'bg-rose-500/20 text-rose-400 border-rose-500/40' },
    { label: 'Carta Scaduta', pan: '4532••••1098', amount: 10.98, expectedResult: 'Simula Errore 54', badgeColor: 'bg-amber-500/20 text-amber-400 border-amber-500/40' },
    { label: 'Sospetto Frode', pan: '4532••••1097', amount: 10.97, expectedResult: 'Simula Errore 59', badgeColor: 'bg-purple-500/20 text-purple-400 border-purple-500/40' },
  ];

  const selectPreset = (preset: PresetCard) => {
    setAmount(preset.amount.toString());
    setCardNumber(preset.pan.includes('99') ? '4532 0000 0000 1099' : preset.pan.includes('98') ? '4532 0000 0000 1098' : preset.pan.includes('97') ? '4532 0000 0000 1097' : '4242 4242 4242 4242');
    toast.info(`Carta di test applicata: ${preset.label}`);
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
      await new Promise(r => setTimeout(r, 600));
      const generatedToken = `tok_vault_${Math.random().toString(36).substring(2, 10)}`;
      setActiveToken(generatedToken);
      setStep(2);

      await new Promise(r => setTimeout(r, 700));
      const isFailedRule = amountInCents % 100 === 99 || amountInCents % 100 === 98 || amountInCents % 100 === 97;

      const payment = await paymentApi.createPaymentIntent({
        amountCents: amountInCents,
        currency: 'EUR',
        description,
        customerEmail,
        isTest,
      });

      if (isFailedRule) {
        const failureReason = amountInCents % 100 === 99 ? 'Fondi insufficienti sulla carta (Codice errore: 51)' : amountInCents % 100 === 98 ? 'Carta di credito scaduta (Codice errore: 54)' : 'Sospetto frode bloccato dalla banca (Codice errore: 59)';
        const failedPayment: PaymentIntent = {
          ...payment,
          status: 'FAILED',
          failureReason,
        };
        setCompletedPayment(failedPayment);
        setStep(5);
        toast.error(`Pagamento Rifiutato: ${failureReason}`);
        return;
      }

      const succeededPayment: PaymentIntent = {
        ...payment,
        status: 'SUCCEEDED',
        authorizationCode: `AUTH_${Math.floor(100000 + Math.random() * 900000)}`,
        bankTransactionId: `tx_bank_${Math.random().toString(36).substring(2, 9)}`,
      };
      setCompletedPayment(succeededPayment);
      setStep(3);

      await new Promise(r => setTimeout(r, 500));
      setStep(4);

      await new Promise(r => setTimeout(r, 600));
      const invoiceData = await invoiceApi.getDownloadUrl(`inv_${succeededPayment.id}`);
      setInvoiceUrl(invoiceData.downloadUrl);
      setStep(5);

      toast.success('Pagamento elaborato ed autorizzato con successo!');
    } catch (err) {
      toast.error('Errore durante la simulazione di pagamento');
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
    bankTransactionId: completedPayment.bankTransactionId || null,
    failureReason: completedPayment.failureReason || null,
    isTest,
  }, null, 2) : '';

  const copyJsonPayload = () => {
    navigator.clipboard.writeText(currentPayloadJson);
    setCopiedPayload(true);
    toast.success('Payload copiato negli appunti!');
    setTimeout(() => setCopiedPayload(false), 2000);
  };

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Title */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight flex items-center gap-3">
            <span>Checkout Demo E-Commerce</span>
            <span className="text-xs font-semibold px-3 py-1 rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/40">
              Stripe Elements Experience
            </span>
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Simula un acquisto reale lato cliente e verifica l'orchestrazione automatica tra i microservizi.
          </p>
        </div>
      </div>

      {/* Preset Test Cards Selection */}
      <div className="glass-panel p-4 space-y-3">
        <div className="flex items-center gap-2 text-xs font-semibold text-slate-300">
          <Sparkles className="w-4 h-4 text-amber-400" />
          <span>Carte di Test Rapide (Simulazione Esiti Bancari)</span>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {presetCards.map((preset, idx) => (
            <button
              key={idx}
              type="button"
              onClick={() => selectPreset(preset)}
              className="glass-card p-3 text-left hover:scale-[1.02] transition-transform duration-200"
            >
              <div className="text-xs font-bold text-slate-200">{preset.label}</div>
              <div className="text-[11px] text-slate-400 mt-0.5">€ {preset.amount.toFixed(2)}</div>
              <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full inline-block mt-2 border ${preset.badgeColor}`}>
                {preset.expectedResult}
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Main Checkout Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left: Order Summary */}
        <div className="glass-panel p-6 space-y-6 flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center gap-2 pb-3 border-b border-slate-800 text-white font-bold text-base">
              <ShoppingBag className="w-5 h-5 text-indigo-400" />
              <span>Riepilogo Ordine</span>
            </div>

            <div className="space-y-3 text-xs">
              <div className="flex justify-between text-slate-300">
                <span>Esercente:</span>
                <span className="font-semibold text-white">{merchant.businessName}</span>
              </div>
              <div className="flex justify-between text-slate-300">
                <span>Prodotto / Servizio:</span>
                <span className="font-semibold text-white">{description}</span>
              </div>
              <div className="pt-3 border-t border-slate-800 flex justify-between items-center">
                <span className="text-sm font-bold text-slate-200">Totale da Pagare:</span>
                <span className="text-xl font-extrabold text-emerald-400 font-mono">€ {parseFloat(amount || '0').toFixed(2)}</span>
              </div>
            </div>
          </div>

          <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-[11px] text-slate-400 flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>Pagamento protetto da cifratura TLS ed isolamento PCI-DSS Vault.</span>
          </div>
        </div>

        {/* Right: Payment Form */}
        <div className="glass-panel p-6 lg:col-span-2 space-y-6">
          <div className="flex items-center justify-between pb-3 border-b border-slate-800">
            <div className="flex items-center gap-2">
              <CreditCardIcon className="w-5 h-5 text-indigo-400" />
              <h2 className="text-base font-bold text-white">Dettagli Carta di Credito</h2>
            </div>
            <span className="text-xs font-mono text-slate-400">Modalità {isTest ? 'Sandbox' : 'Live'}</span>
          </div>

          <form onSubmit={handleSimulatePayment} className="space-y-4 text-xs">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-slate-300 font-semibold mb-1">Importo (€)</label>
                <input
                  type="number"
                  step="0.01"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="glass-input w-full font-bold text-sm text-emerald-400"
                  required
                />
              </div>

              <div>
                <label className="block text-slate-300 font-semibold mb-1">Email Cliente</label>
                <input
                  type="email"
                  value={customerEmail}
                  onChange={(e) => setCustomerEmail(e.target.value)}
                  className="glass-input w-full"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-slate-300 font-semibold mb-1">Descrizione Pagamento</label>
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="glass-input w-full"
                required
              />
            </div>

            <div>
              <label className="block text-slate-300 font-semibold mb-1">Titolare della Carta</label>
              <input
                type="text"
                value={cardHolder}
                onChange={(e) => setCardHolder(e.target.value)}
                className="glass-input w-full"
                required
              />
            </div>

            <div>
              <label className="block text-slate-300 font-semibold mb-1">Numero Carta (PAN)</label>
              <div className="relative">
                <input
                  type="text"
                  value={cardNumber}
                  onChange={(e) => setCardNumber(e.target.value)}
                  className="glass-input w-full font-mono text-slate-100"
                  required
                />
                <span className="absolute right-3 top-2.5 text-[10px] font-bold px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                  {cardNumber.startsWith('4') ? 'VISA' : 'MASTERCARD'}
                </span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-slate-300 font-semibold mb-1">Scadenza</label>
                <input
                  type="text"
                  value={expiry}
                  onChange={(e) => setExpiry(e.target.value)}
                  placeholder="MM/YY"
                  className="glass-input w-full font-mono text-center"
                  required
                />
              </div>
              <div>
                <label className="block text-slate-300 font-semibold mb-1">CVC / CVV</label>
                <input
                  type="text"
                  maxLength={4}
                  value={cvv}
                  onChange={(e) => setCvv(e.target.value)}
                  className="glass-input w-full font-mono text-center"
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={processing}
              className="btn-primary w-full text-xs font-semibold py-3 mt-4 flex items-center justify-center gap-2"
            >
              <span>{processing ? 'Elaborazione in corso...' : `Paga Ora (€ ${parseFloat(amount || '0').toFixed(2)})`}</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          {/* Stepline Visualizer */}
          {step > 0 && (
            <div className="pt-4 space-y-3 border-t border-slate-800">
              <h3 className="text-xs font-bold text-slate-200">Stato Orchestrazione Microservizi</h3>
              
              <div className="space-y-2 text-xs">
                <div className={`p-3 rounded-xl border flex items-center justify-between ${step >= 1 ? 'bg-indigo-950/40 border-indigo-500/40 text-indigo-300' : 'bg-slate-950/40 border-slate-800 text-slate-500'}`}>
                  <span>1. Tokenizzazione Sicura Carta (`aura-vault-service`)</span>
                  {activeToken && <span className="font-mono text-[10px] text-cyan-300">{activeToken}</span>}
                </div>

                <div className={`p-3 rounded-xl border flex items-center justify-between ${step >= 2 ? 'bg-indigo-950/40 border-indigo-500/40 text-indigo-300' : 'bg-slate-950/40 border-slate-800 text-slate-500'}`}>
                  <span>2. Autorizzazione Bancaria (`aura-payment-orchestrator`)</span>
                  {completedPayment && (
                    <span className={`font-mono text-[10px] font-bold px-2 py-0.5 rounded border ${completedPayment.status === 'SUCCEEDED' ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30' : 'bg-rose-500/20 text-rose-400 border-rose-500/30'}`}>
                      {completedPayment.status === 'SUCCEEDED' ? 'APPROVATO' : 'RIFIUTATO'}
                    </span>
                  )}
                </div>

                <div className={`p-3 rounded-xl border flex items-center justify-between ${step >= 3 ? 'bg-indigo-950/40 border-indigo-500/40 text-indigo-300' : 'bg-slate-950/40 border-slate-800 text-slate-500'}`}>
                  <span>3. Registrazione Contabile Ledger (`aura-ledger-service`)</span>
                  {step >= 3 && <CheckCircle2 className="w-4 h-4 text-emerald-400" />}
                </div>

                <div className={`p-3 rounded-xl border flex items-center justify-between ${step >= 4 ? 'bg-indigo-950/40 border-indigo-500/40 text-indigo-300' : 'bg-slate-950/40 border-slate-800 text-slate-500'}`}>
                  <span>4. Emissione Fattura PDF su MinIO S3 (`aura-invoice-service`)</span>
                  {invoiceUrl && (
                    <a href={invoiceUrl} target="_blank" rel="noreferrer" className="text-indigo-400 hover:underline flex items-center gap-1">
                      <FileText className="w-3.5 h-3.5" />
                      <span>Scarica PDF</span>
                    </a>
                  )}
                </div>

                <div className={`p-3 rounded-xl border flex items-center justify-between ${step >= 5 ? 'bg-indigo-950/40 border-indigo-500/40 text-indigo-300' : 'bg-slate-950/40 border-slate-800 text-slate-500'}`}>
                  <span>5. Notifica Webhook HMAC (`aura-webhook-service`)</span>
                  {step >= 5 && <Webhook className="w-4 h-4 text-emerald-400" />}
                </div>
              </div>

              {/* Dev Payload Toggle */}
              {completedPayment && (
                <div className="pt-2">
                  <button
                    onClick={() => setShowDevInspector(!showDevInspector)}
                    className="text-xs font-semibold text-slate-400 hover:text-white flex items-center gap-1"
                  >
                    <Code2 className="w-4 h-4 text-indigo-400" />
                    <span>{showDevInspector ? 'Nascondi Payload Dev' : 'Ispeziona Payload Kafka Event (Dev)'}</span>
                    {showDevInspector ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                  </button>

                  {showDevInspector && (
                    <div className="mt-3 relative">
                      <button onClick={copyJsonPayload} className="absolute right-3 top-3 text-slate-400 hover:text-white flex items-center gap-1 text-[10px]">
                        {copiedPayload ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                        <span>{copiedPayload ? 'Copiato!' : 'Copia'}</span>
                      </button>
                      <pre className="p-4 rounded-xl bg-slate-950 border border-slate-800 font-mono text-[11px] text-emerald-400 overflow-x-auto">
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
