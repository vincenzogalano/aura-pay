import React, { useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { merchantApi } from '../api/merchantApi';
import { 
  UserCheck, 
  Building2, 
  Mail, 
  Globe, 
  FileText, 
  ShieldAlert, 
  ShieldCheck, 
  Sparkles
} from 'lucide-react';
import { toast } from 'sonner';

export const OnboardingPage: React.FC = () => {
  const { merchant, updateMerchant, addApiKey } = useMerchant();

  // Registration Form State
  const [businessName, setBusinessName] = useState<string>(merchant.businessName);
  const [vatNumber, setVatNumber] = useState<string>(merchant.vatNumber);
  const [email, setEmail] = useState<string>(merchant.email);
  const [country, setCountry] = useState<string>(merchant.country || 'IT');
  const [registering, setRegistering] = useState<boolean>(false);

  // KYB State
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
      toast.success('Registrazione completata! Chiavi Sandbox generate con successo.');
    } catch (err) {
      toast.error('Errore durante la registrazione merchant');
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

      // Se verificato, generiamo automaticamente le chiavi live nel mock
      if (updatedMerchant.status === 'VERIFIED') {
        const liveKey = await merchantApi.generateLiveKeys(merchant.id);
        addApiKey(liveKey);
        toast.success('Verifica KYB approvata! Le chiavi LIVE sono ora disponibili.');
      } else {
        toast.warning('Richiesta KYB in lavorazione.');
      }
    } catch (err) {
      toast.error('Errore durante l\'invio della verifica KYB');
    } finally {
      setSubmittingKYB(false);
    }
  };

  return (
    <div className="space-y-8 animate-fadeIn max-w-5xl">
      {/* Title */}
      <div>
        <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
          Onboarding & Profilo Merchant
        </h1>
        <p className="text-slate-400 text-sm mt-1">
          Gestisci l'anagrafica aziendale e completa la verifica KYB per sbloccare i pagamenti reali in ambiente LIVE.
        </p>
      </div>

      {/* Status Banner */}
      <div className={`glass-panel p-6 border flex items-center justify-between gap-4 ${
        merchant.status === 'VERIFIED'
          ? 'border-emerald-500/30 bg-emerald-950/20'
          : merchant.status === 'VERIFICATION_REJECTED'
          ? 'border-rose-500/30 bg-rose-950/20'
          : 'border-amber-500/30 bg-amber-950/20'
      }`}>
        <div className="flex items-center gap-4">
          <div className={`p-3 rounded-2xl ${
            merchant.status === 'VERIFIED' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-amber-500/20 text-amber-400'
          }`}>
            {merchant.status === 'VERIFIED' ? <ShieldCheck className="w-8 h-8" /> : <ShieldAlert className="w-8 h-8" />}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-lg font-bold text-white">Stato Merchant:</h2>
              <span className={`text-xs font-extrabold uppercase px-3 py-1 rounded-full ${
                merchant.status === 'VERIFIED' ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40' : 'bg-amber-500/20 text-amber-400 border border-amber-500/40'
              }`}>
                {merchant.status}
              </span>
            </div>
            <p className="text-xs text-slate-300 mt-1">
              {merchant.status === 'VERIFIED'
                ? 'La tua azienda è verificata. Puoi operare sia in Sandbox sia in ambiente LIVE con denaro reale.'
                : 'Puoi operare subito in ambiente SANDBOX. Completa il widget KYB sottostante per abilitare la modalità LIVE.'}
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Form Registrazione Self-Service */}
        <div className="glass-panel p-6 space-y-6">
          <div className="flex items-center gap-3 pb-3 border-b border-slate-800">
            <Building2 className="w-5 h-5 text-indigo-400" />
            <h3 className="text-lg font-bold text-white">Dati Anagrafici Azienda</h3>
          </div>

          <form onSubmit={handleRegister} className="space-y-4 text-xs">
            <div>
              <label className="block text-slate-300 font-semibold mb-1">Ragione Sociale</label>
              <div className="relative">
                <input
                  type="text"
                  value={businessName}
                  onChange={(e) => setBusinessName(e.target.value)}
                  className="glass-input w-full pl-9"
                  required
                />
                <Building2 className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
              </div>
            </div>

            <div>
              <label className="block text-slate-300 font-semibold mb-1">Partita IVA / Tax Code</label>
              <div className="relative">
                <input
                  type="text"
                  value={vatNumber}
                  onChange={(e) => setVatNumber(e.target.value)}
                  className="glass-input w-full pl-9 font-mono"
                  required
                />
                <FileText className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
              </div>
            </div>

            <div>
              <label className="block text-slate-300 font-semibold mb-1">Email Aziendale</label>
              <div className="relative">
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="glass-input w-full pl-9"
                  required
                />
                <Mail className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
              </div>
            </div>

            <div>
              <label className="block text-slate-300 font-semibold mb-1">Paese di Sede</label>
              <div className="relative">
                <select
                  value={country}
                  onChange={(e) => setCountry(e.target.value)}
                  className="glass-input w-full pl-9 cursor-pointer"
                >
                  <option value="IT">Italia (IT)</option>
                  <option value="DE">Germania (DE)</option>
                  <option value="FR">Francia (FR)</option>
                  <option value="ES">Spagna (ES)</option>
                </select>
                <Globe className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
              </div>
            </div>

            <button
              type="submit"
              disabled={registering}
              className="btn-primary w-full text-xs font-semibold py-2.5 mt-2"
            >
              {registering ? 'Aggiornamento in corso...' : 'Salva Anagrafica Merchant'}
            </button>
          </form>
        </div>

        {/* Widget KYB Verification */}
        <div className="glass-panel p-6 space-y-6 flex flex-col justify-between">
          <div>
            <div className="flex items-center gap-3 pb-3 border-b border-slate-800">
              <UserCheck className="w-5 h-5 text-emerald-400" />
              <h3 className="text-lg font-bold text-white">Widget Verifica KYB (Know Your Business)</h3>
            </div>

            <p className="text-xs text-slate-400 mt-4 leading-relaxed">
              La verifica KYB controlla i dati fiscali dell'azienda e del legale rappresentante per garantire la conformità PCI-DSS e prevenire frodi finanziarie.
            </p>

            <form onSubmit={handleRequestKYB} className="space-y-4 text-xs mt-4">
              <div>
                <label className="block text-slate-300 font-semibold mb-1">Indirizzo Sede Legale</label>
                <input
                  type="text"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  className="glass-input w-full"
                  required
                />
              </div>

              <div>
                <label className="block text-slate-300 font-semibold mb-1">Nome e Cognome Legale Rappresentante</label>
                <input
                  type="text"
                  value={legalRep}
                  onChange={(e) => setLegalRep(e.target.value)}
                  className="glass-input w-full"
                  required
                />
              </div>

              <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800 space-y-1">
                <div className="text-[11px] font-semibold text-slate-300 flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-amber-400" />
                  <span>Simulazione Approvazione Automatica</span>
                </div>
                <p className="text-[10px] text-slate-500">
                  Nel motore di test AuraPay, le aziende con P.IVA e indirizzo compilati vengono approvate automaticamente in tempo reale.
                </p>
              </div>

              <button
                type="submit"
                disabled={submittingKYB || merchant.status === 'VERIFIED'}
                className="bg-emerald-600 hover:bg-emerald-500 text-white font-medium px-4 py-2.5 rounded-lg text-xs w-full transition-colors shadow-lg shadow-emerald-600/30 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {merchant.status === 'VERIFIED'
                  ? 'Verifica KYB già Completata ✓'
                  : submittingKYB
                  ? 'Verifica in corso...'
                  : 'Invia Richiesta di Attivazione LIVE'}
              </button>
            </form>
          </div>

          <div className="pt-4 border-t border-slate-800/60 flex items-center justify-between text-xs text-slate-400">
            <span>Ambiente Live sbloccabile:</span>
            <span className="font-mono text-emerald-400 font-bold">sk_live_...</span>
          </div>
        </div>
      </div>
    </div>
  );
};
