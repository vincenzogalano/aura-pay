import React, { useState, useEffect } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { merchantApi } from '../api/merchantApi';
import { 
  Building2, 
  FileText, 
  ShieldAlert, 
  ShieldCheck,
  UserCheck,
  UserPlus,
  Save,
  CheckCircle2,
  Info
} from 'lucide-react';
import { getKYBStatusInfo } from '../utils/statusUtils';
import { toast } from 'sonner';

export const OnboardingPage: React.FC = () => {
  const { merchant, updateMerchant, setMerchantProfile, addApiKey } = useMerchant();

  // Form 1: Registrazione Nuovo Merchant (POST /v1/merchants/register)
  const [newBusinessName, setNewBusinessName] = useState<string>('');
  const [newVatNumber, setNewVatNumber] = useState<string>('IT99887766554');
  const [newEmail, setNewEmail] = useState<string>('contabilita@nuovomarchio.it');
  const [registering, setRegistering] = useState<boolean>(false);

  // Form 2: Modifica Profilo Merchant Selezionato (PUT /v1/merchants/{id})
  const [editBusinessName, setEditBusinessName] = useState<string>(merchant.businessName);
  const [editEmail, setEditEmail] = useState<string>(merchant.email);
  const [updatingProfile, setUpdatingProfile] = useState<boolean>(false);

  // Form 3: Verifica KYB Merchant Selezionato (POST /v1/merchants/{id}/verification-request)
  const [address, setAddress] = useState<string>('Via Roma 100, Milano (MI)');
  const [legalRep, setLegalRep] = useState<string>('Mario Rossi');
  const [submittingKYB, setSubmittingKYB] = useState<boolean>(false);

  // Sincronizza i campi di modifica profilo quando cambia il merchant selezionato
  useEffect(() => {
    setEditBusinessName(merchant.businessName);
    setEditEmail(merchant.email);
  }, [merchant.id, merchant.businessName, merchant.email]);

  // Handler 1: Registrazione Nuovo Merchant Reale nel DB
  const handleRegisterNewMerchant = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newBusinessName.trim()) {
      toast.error('Inserisci la Ragione Sociale del nuovo esercente');
      return;
    }
    setRegistering(true);
    try {
      const res = await merchantApi.registerMerchant({
        businessName: newBusinessName,
        vatNumber: newVatNumber,
        email: newEmail,
        country: 'IT',
      });
      setMerchantProfile(res.merchant);
      if (res.apiKeys) {
        res.apiKeys.forEach((k) => addApiKey(k));
      }
      setNewBusinessName('');
      toast.success(`Nuovo Esercente "${res.merchant.businessName}" salvato nel DB PostgreSQL!`);
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Errore durante la registrazione nel DB';
      toast.error(msg);
    } finally {
      setRegistering(false);
    }
  };

  // Handler 2: Modifica Dati Esercente Corrente nel DB
  const handleUpdateCurrentMerchant = async (e: React.FormEvent) => {
    e.preventDefault();
    setUpdatingProfile(true);
    try {
      const updated = await merchantApi.updateMerchantProfile(merchant.id, {
        businessName: editBusinessName,
        email: editEmail,
      });
      updateMerchant(updated);
      toast.success(`Dati dell'esercente "${updated.businessName}" aggiornati nel DB!`);
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Errore durante l\'aggiornamento del profilo';
      toast.error(msg);
    } finally {
      setUpdatingProfile(false);
    }
  };

  // Handler 3: Richiesta Verifica KYB
  const handleRequestKYB = async (e?: React.FormEvent, forceReject = false) => {
    if (e) e.preventDefault();
    setSubmittingKYB(true);
    try {
      const payload = forceReject
        ? { registrationNumber: 'REJECT_KYB_FAULT_INJECTION', businessAddress: address, legalRepresentative: legalRep }
        : { registrationNumber: 'REG_123456_OK', businessAddress: address, legalRepresentative: legalRep };

      const updatedMerchant = await merchantApi.requestKYBVerification(merchant.id, payload);
      updateMerchant(updatedMerchant);

      if (updatedMerchant.status === 'VERIFIED') {
        const liveKey = await merchantApi.generateLiveKeys(merchant.id);
        if (Array.isArray(liveKey)) liveKey.forEach((k) => addApiKey(k));
        toast.success('Verifica KYB completata! Evento Kafka aura.merchant.verified.v1 pubblicato.');
      } else if (updatedMerchant.status === 'VERIFICATION_REJECTED') {
        toast.warning('Verifica KYB Rifiutata! Evento Kafka aura.merchant.verification_rejected.v1 pubblicato.');
      }
    } catch (err) {
      toast.error('Errore durante l\'invio della verifica KYB');
    } finally {
      setSubmittingKYB(false);
    }
  };

  return (
    <div className="space-y-6 max-w-5xl animate-fadeIn">
      {/* Header Page Title */}
      <div className="border-b border-zinc-200 pb-5">
        <h1 className="text-xl font-bold text-zinc-900 tracking-tight flex items-center gap-2">
          <UserCheck className="w-5 h-5 text-indigo-600" />
          <span>Gestione Esercenti &amp; Verifica KYB</span>
        </h1>
        <p className="text-zinc-500 text-xs mt-0.5">
          Registra nuovi tenant esercenti nel DB PostgreSQL, modifica l'anagrafica corrente e gestisci la verifica fiscale KYB per la modalità Live.
        </p>
      </div>

      {/* Status Banner Merchant Attivo */}
      <div className="p-4 rounded-lg bg-zinc-900 border border-zinc-800 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          {merchant.status === 'VERIFIED' ? (
            <ShieldCheck className="w-6 h-6 text-emerald-400 shrink-0" />
          ) : (
            <ShieldAlert className="w-6 h-6 text-amber-400 shrink-0" />
          )}
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-xs text-zinc-400">Esercente Selezionato:</span>
              <span className="text-sm font-bold text-zinc-100">{merchant.businessName}</span>
              {(() => {
                const kybInfo = getKYBStatusInfo(merchant.status);
                return (
                  <span 
                    title={kybInfo.description}
                    className={`text-[10px] font-sans font-semibold px-2 py-0.5 rounded border inline-flex items-center gap-1 ${kybInfo.bgClass} ${kybInfo.textClass} ${kybInfo.borderClass}`}
                  >
                    <span>{kybInfo.icon}</span>
                    <span>{kybInfo.label}</span>
                  </span>
                );
              })()}
            </div>
          </div>
        </div>
      </div>

      {/* Guida Informativa Registrazione & KYB */}
      <div className="p-4 rounded-lg bg-indigo-950/40 border border-indigo-800/60 text-zinc-200 text-xs space-y-2.5">
        <div className="flex items-center gap-2 font-bold text-indigo-300">
          <Info className="w-4 h-4 text-indigo-400 shrink-0" />
          <span>Guida alla Registrazione Multi-Tenant &amp; Verifica Fiscalità KYB</span>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-1 text-[11px] leading-relaxed text-zinc-300">
          <div className="p-2.5 rounded bg-zinc-900/50 border border-indigo-900/40 space-y-1">
            <strong className="text-indigo-300 block font-semibold">1. Registrazione Nuovo Merchant</strong>
            <span>
              Inserisci la Ragione Sociale, una Partita IVA (es. <code className="font-mono text-amber-300">IT12345678901</code>) ed un'Email Aziendale. Il nuovo tenant viene salvato nel DB Postgres ed emette l'evento Kafka <code className="font-mono text-indigo-300">aura.merchant.created.v1</code>.
            </span>
          </div>
          <div className="p-2.5 rounded bg-zinc-900/50 border border-indigo-900/40 space-y-1">
            <strong className="text-indigo-300 block font-semibold">2. Switch Esercente in Topbar</strong>
            <span>
              Tutti gli esercenti creati sono subito selezionabili tramite la tendina in alto nel menu principale. Ogni esercente ha i suoi dati e transazioni isolate su DB.
            </span>
          </div>
          <div className="p-2.5 rounded bg-zinc-900/50 border border-indigo-900/40 space-y-1">
            <strong className="text-indigo-300 block font-semibold">3. Approvazione vs Rifiuto KYB</strong>
            <span>
              Cliccando <strong>"Completa Verifica KYB"</strong> l'esercente viene promosso a <code className="font-mono text-emerald-400">VERIFIED</code>, genera le chiavi <code className="font-mono text-emerald-400">sk_live_...</code> ed emette l'evento Kafka <code className="font-mono text-emerald-400">aura.merchant.verified.v1</code>.
            </span>
          </div>
        </div>
      </div>

      {/* Grid delle 3 Sezioni Principali */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* SECTION 1: Registrazione Nuovo Merchant */}
        <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs space-y-4 flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center gap-2 pb-3 border-b border-zinc-100 text-xs font-bold text-zinc-900">
              <UserPlus className="w-4 h-4 text-indigo-600" />
              <span>1. Registra Nuovo Esercente</span>
            </div>

            <p className="text-[11px] text-zinc-500">
              Crea un nuovo tenant nel DB PostgreSQL (<code className="font-mono text-zinc-800">POST /v1/merchants/register</code>). Emette l'evento Kafka <code className="font-mono text-indigo-600">aura.merchant.created.v1</code>.
            </p>

            <div className="p-2.5 rounded bg-amber-50 border border-amber-200 text-[11px] text-amber-900 space-y-1">
              <span className="font-semibold block text-amber-950">💡 Formati Dati Accettati:</span>
              <div>• <strong>P.IVA</strong>: Qualsiasi stringa di test (es. <code className="font-mono text-amber-800">IT12345678901</code>, <code className="font-mono text-amber-800">IT12345666554</code>).</div>
              <div>• <strong>Email</strong>: Qualsiasi indirizzo aziendale (es. <code className="font-mono text-amber-800">vincenzo.galano@test.it</code>).</div>
            </div>

            <form onSubmit={handleRegisterNewMerchant} className="space-y-3 text-xs">
              <div>
                <label className="block text-zinc-700 font-medium mb-1">Ragione Sociale *</label>
                <input
                  type="text"
                  placeholder="Es. E-Commerce Italia S.r.l."
                  value={newBusinessName}
                  onChange={(e) => setNewBusinessName(e.target.value)}
                  className="shadcn-input w-full"
                  required
                />
              </div>

              <div>
                <label className="block text-zinc-700 font-medium mb-1">Partita IVA / Codice Fiscale *</label>
                <input
                  type="text"
                  value={newVatNumber}
                  onChange={(e) => setNewVatNumber(e.target.value)}
                  className="shadcn-input w-full font-mono"
                  required
                />
              </div>

              <div>
                <label className="block text-zinc-700 font-medium mb-1">Email Aziendale *</label>
                <input
                  type="email"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                  className="shadcn-input w-full"
                  required
                />
              </div>

              <button
                type="submit"
                disabled={registering}
                className="btn-shadcn-primary w-full text-xs py-2 mt-2 flex items-center justify-center gap-1.5"
              >
                <UserPlus className="w-3.5 h-3.5" />
                <span>{registering ? 'Registrazione in corso...' : 'Registra Esercente nel DB'}</span>
              </button>
            </form>
          </div>
        </div>

        {/* SECTION 2: Modifica Anagrafica Esercente Selezionato */}
        <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs space-y-4 flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-zinc-100 text-xs font-bold text-zinc-900">
              <div className="flex items-center gap-2">
                <Building2 className="w-4 h-4 text-emerald-600" />
                <span>2. Modifica Esercente Attivo</span>
              </div>
            </div>

            <p className="text-[11px] text-zinc-500">
              Aggiorna l'anagrafica dell'esercente corrente nel DB PostgreSQL (<code className="font-mono text-zinc-800">PUT /v1/merchants/{'{id}'}</code>).
            </p>

            <form onSubmit={handleUpdateCurrentMerchant} className="space-y-3 text-xs">
              <div>
                <label className="block text-zinc-700 font-medium mb-1">Ragione Sociale</label>
                <input
                  type="text"
                  value={editBusinessName}
                  onChange={(e) => setEditBusinessName(e.target.value)}
                  className="shadcn-input w-full font-medium"
                  required
                />
              </div>

              <div>
                <label className="block text-zinc-700 font-medium mb-1">Email Aziendale</label>
                <input
                  type="email"
                  value={editEmail}
                  onChange={(e) => setEditEmail(e.target.value)}
                  className="shadcn-input w-full"
                  required
                />
              </div>

              <div>
                <label className="block text-zinc-400 font-medium mb-1">Partita IVA (Immutabile)</label>
                <input
                  type="text"
                  value={merchant.vatNumber}
                  disabled
                  className="shadcn-input w-full font-mono bg-zinc-100 text-zinc-500 cursor-not-allowed"
                />
              </div>

              <button
                type="submit"
                disabled={updatingProfile}
                className="btn-shadcn-secondary w-full text-xs py-2 mt-2 flex items-center justify-center gap-1.5"
              >
                <Save className="w-3.5 h-3.5" />
                <span>{updatingProfile ? 'Salvataggio...' : 'Salva Modifiche Profilo'}</span>
              </button>
            </form>
          </div>
        </div>

        {/* SECTION 3: Verifica KYB & Sblocco Produzione Live */}
        <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs space-y-4 flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center gap-2 pb-3 border-b border-zinc-100 text-xs font-bold text-zinc-900">
              <FileText className="w-4 h-4 text-amber-600" />
              <span>3. Verifica Fiscalità KYB</span>
            </div>

            <p className="text-[11px] text-zinc-500">
              Invia la verifica fiscale per sbloccare la modalità **LIVE** (<code className="font-mono text-zinc-800">POST /v1/merchants/{'{id}'}/verification-request</code>).
            </p>

            <form onSubmit={(e) => handleRequestKYB(e, false)} className="space-y-3 text-xs">
              <div>
                <label className="block text-zinc-700 font-medium mb-1">Indirizzo Sede Legale</label>
                <input
                  type="text"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  className="shadcn-input w-full"
                  required
                />
              </div>

              <div>
                <label className="block text-zinc-700 font-medium mb-1">Legale Rappresentante</label>
                <input
                  type="text"
                  value={legalRep}
                  onChange={(e) => setLegalRep(e.target.value)}
                  className="shadcn-input w-full"
                  required
                />
              </div>

              <div className="p-2.5 rounded bg-indigo-50 border border-indigo-200 text-[11px] text-indigo-950 space-y-1">
                <span className="font-semibold block text-indigo-950">💡 Esito Verifica Fiscalità:</span>
                <div>• <strong>Approvazione</strong>: Promuove l'esercente a <span className="font-semibold text-emerald-700">VERIFIED</span>, genera le chiavi <code className="font-mono text-indigo-800">sk_live_...</code> ed emette l'evento Kafka <code className="font-mono text-indigo-800">aura.merchant.verified.v1</code>.</div>
                <div>• <strong>Rifiuto</strong>: Simula l'evento di fault injection <code className="font-mono text-rose-700">aura.merchant.verification_rejected.v1</code>.</div>
              </div>

              <div className="space-y-2 pt-2">
                <button
                  type="submit"
                  disabled={submittingKYB || merchant.status === 'VERIFIED'}
                  className="btn-shadcn-primary w-full text-xs py-2 disabled:opacity-50 flex items-center justify-center gap-1.5"
                >
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>
                    {merchant.status === 'VERIFIED'
                      ? 'Verifica Completata ✓'
                      : submittingKYB
                      ? 'Verifica in corso...'
                      : 'Completa Verifica KYB (Approvato)'}
                  </span>
                </button>

                <button
                  type="button"
                  onClick={(e) => handleRequestKYB(e, true)}
                  disabled={submittingKYB}
                  className="btn-shadcn-secondary w-full text-xs py-1.5 text-rose-600 border-rose-200 bg-rose-50 hover:bg-rose-100 transition-colors"
                >
                  🔴 Simula Rifiuto KYB (Fault Injection Test)
                </button>
              </div>
            </form>
          </div>
        </div>

      </div>
    </div>
  );
};
