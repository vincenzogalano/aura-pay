import React, { useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { merchantApi } from '../api/merchantApi';
import { 
  Building2, 
  FileText, 
  ShieldAlert, 
  ShieldCheck
} from 'lucide-react';
import { toast } from 'sonner';

export const OnboardingPage: React.FC = () => {
  const { merchant, updateMerchant, addApiKey } = useMerchant();

  const [businessName, setBusinessName] = useState<string>(merchant.businessName);
  const [vatNumber, setVatNumber] = useState<string>(merchant.vatNumber);
  const [email, setEmail] = useState<string>(merchant.email);
  const [country] = useState<string>(merchant.country || 'IT');
  const [registering, setRegistering] = useState<boolean>(false);

  const [address, setAddress] = useState<string>('Via Roma 100, Milano (MI)');
  const [legalRep, setLegalRep] = useState<string>('Mario Rossi');
  const [submittingKYB, setSubmittingKYB] = useState<boolean>(false);

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setRegistering(true);
    try {
      const res = await merchantApi.registerMerchant({ businessName, vatNumber, email, country });
      updateMerchant(res.merchant);
      res.apiKeys.forEach(k => addApiKey(k));
      toast.success('Dati aggiornati!');
    } catch (err) {
      toast.error('Errore durante il salvataggio');
    } finally {
      setRegistering(false);
    }
  };

  const handleRequestKYB = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmittingKYB(true);
    try {
      const updatedMerchant = await merchantApi.requestKYBVerification(merchant.id, {
        taxId: vatNumber,
        address,
        legalRepresentative: legalRep,
      });
      updateMerchant(updatedMerchant);

      if (updatedMerchant.status === 'VERIFIED') {
        const liveKey = await merchantApi.generateLiveKeys(merchant.id);
        addApiKey(liveKey);
        toast.success('Verifica KYB completata! Modalità Live abilitata.');
      }
    } catch (err) {
      toast.error('Errore durante l\'invio');
    } finally {
      setSubmittingKYB(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl animate-fadeIn">
      {/* Title */}
      <div className="border-b border-zinc-800 pb-5">
        <h1 className="text-xl font-bold text-zinc-100 tracking-tight">
          Registrazione & Verifica KYB
        </h1>
        <p className="text-zinc-400 text-xs mt-0.5">
          Gestisci le informazioni societarie e sblocca la modalità Live in produzione.
        </p>
      </div>

      {/* Status Banner */}
      <div className="p-4 rounded-lg bg-zinc-900 border border-zinc-800 flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          {merchant.status === 'VERIFIED' ? (
            <ShieldCheck className="w-5 h-5 text-emerald-400" />
          ) : (
            <ShieldAlert className="w-5 h-5 text-amber-400" />
          )}
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-zinc-200">Stato Account:</span>
              <span className={`text-[10px] font-mono font-semibold px-2 py-0.5 rounded border ${
                merchant.status === 'VERIFIED' ? 'bg-emerald-950 text-emerald-400 border-emerald-800' : 'bg-amber-950 text-amber-400 border-amber-800'
              }`}>
                {merchant.status === 'VERIFIED' ? 'VERIFICATO (LIVE ABILITATO)' : 'PENDING (SOLO SANDBOX)'}
              </span>
            </div>
            <p className="text-xs text-zinc-400 mt-0.5">
              {merchant.status === 'VERIFIED'
                ? 'L\'account è abilitato per incassi in ambiente di produzione Live.'
                : 'Completa la verifica KYB per accedere all\'ambiente Live.'}
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Form Registrazione Self-Service */}
        <div className="p-5 rounded-lg bg-zinc-900/60 border border-zinc-800 space-y-4">
          <div className="flex items-center gap-2 pb-3 border-b border-zinc-800 text-xs font-bold text-zinc-100">
            <Building2 className="w-4 h-4 text-zinc-400" />
            <span>Dati Societari Esercente</span>
          </div>

          <form onSubmit={handleRegister} className="space-y-3.5 text-xs">
            <div>
              <label className="block text-zinc-300 font-medium mb-1">Ragione Sociale</label>
              <input
                type="text"
                value={businessName}
                onChange={(e) => setBusinessName(e.target.value)}
                className="shadcn-input w-full"
                required
              />
            </div>

            <div>
              <label className="block text-zinc-300 font-medium mb-1">Partita IVA / Codice Fiscale</label>
              <input
                type="text"
                value={vatNumber}
                onChange={(e) => setVatNumber(e.target.value)}
                className="shadcn-input w-full font-mono"
                required
              />
            </div>

            <div>
              <label className="block text-zinc-300 font-medium mb-1">Email Aziendale</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="shadcn-input w-full"
                required
              />
            </div>

            <button
              type="submit"
              disabled={registering}
              className="btn-shadcn-secondary w-full text-xs py-2 mt-2"
            >
              {registering ? 'Salvataggio...' : 'Salva Dati Societari'}
            </button>
          </form>
        </div>

        {/* Widget KYB Verification */}
        <div className="p-5 rounded-lg bg-zinc-900/60 border border-zinc-800 space-y-4 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-2 pb-3 border-b border-zinc-800 text-xs font-bold text-zinc-100">
              <FileText className="w-4 h-4 text-zinc-400" />
              <span>Verifica Dati Fiscali (KYB)</span>
            </div>

            <form onSubmit={handleRequestKYB} className="space-y-3.5 text-xs mt-4">
              <div>
                <label className="block text-zinc-300 font-medium mb-1">Indirizzo Sede Legale</label>
                <input
                  type="text"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  className="shadcn-input w-full"
                  required
                />
              </div>

              <div>
                <label className="block text-zinc-300 font-medium mb-1">Legale Rappresentante</label>
                <input
                  type="text"
                  value={legalRep}
                  onChange={(e) => setLegalRep(e.target.value)}
                  className="shadcn-input w-full"
                  required
                />
              </div>

              <button
                type="submit"
                disabled={submittingKYB || merchant.status === 'VERIFIED'}
                className="btn-shadcn-primary w-full text-xs py-2 mt-2 disabled:opacity-50"
              >
                {merchant.status === 'VERIFIED'
                  ? 'Verifica Completata ✓'
                  : submittingKYB
                  ? 'Verifica in corso...'
                  : 'Completa Verifica KYB'}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};
