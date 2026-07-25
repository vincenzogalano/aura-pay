import React, { useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { merchantApi } from '../api/merchantApi';
import { 
  Copy, 
  Eye, 
  EyeOff, 
  ShieldAlert, 
  ShieldCheck, 
  Trash2, 
  Plus, 
  Check 
} from 'lucide-react';
import { toast } from 'sonner';

export const ApiKeysPage: React.FC = () => {
  const { merchant, apiKeys, addApiKey, revokeApiKey } = useMerchant();
  const [revealedKeys, setRevealedKeys] = useState<Record<string, boolean>>({});
  const [copiedKeyId, setCopiedKeyId] = useState<string | null>(null);

  const toggleReveal = (id: string) => {
    setRevealedKeys(prev => ({ ...prev, [id]: !prev[id] }));
  };

  const handleCopy = (id: string, text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKeyId(id);
    toast.success('Chiave API copiata negli appunti!');
    setTimeout(() => setCopiedKeyId(null), 2000);
  };

  const handleRevoke = (id: string) => {
    if (confirm('Sei sicuro di voler revocare questa API Key? I client che la usano perderanno l\'accesso.')) {
      revokeApiKey(id);
      toast.warning('API Key revocata.');
    }
  };

  const handleGenerateLive = async () => {
    if (merchant.status !== 'VERIFIED') {
      toast.error('Devi prima completare la verifica KYB per generare chiavi LIVE.');
      return;
    }
    try {
      const newKey = await merchantApi.generateLiveKeys(merchant.id);
      addApiKey(newKey);
      toast.success('Nuova API Key LIVE generata con successo!');
    } catch (err) {
      toast.error('Errore durante la generazione della chiave LIVE');
    }
  };

  const sandboxKeys = apiKeys.filter(k => k.environment === 'TEST');
  const liveKeys = apiKeys.filter(k => k.environment === 'LIVE');

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Title */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
            Gestione API Keys
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Gestisci le credenziali di autenticazione Server-to-Server per gli ambienti Sandbox e Live.
          </p>
        </div>

        <button
          onClick={handleGenerateLive}
          disabled={merchant.status !== 'VERIFIED'}
          className="btn-primary text-xs flex items-center gap-2"
        >
          <Plus className="w-4 h-4" />
          <span>Genera Nuova Chiave LIVE</span>
        </button>
      </div>

      {/* Sandbox Keys Section */}
      <div className="space-y-4">
        <div className="flex items-center gap-2">
          <ShieldAlert className="w-5 h-5 text-amber-400" />
          <h2 className="text-lg font-bold text-white">Chiavi API Sandbox (Test)</h2>
        </div>

        <div className="glass-panel overflow-hidden">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-[11px] font-semibold text-slate-400 uppercase tracking-wider bg-slate-900/50">
                <th className="py-4 px-6">Prefisso Chiave</th>
                <th className="py-4 px-6">Secret / Masked Key</th>
                <th className="py-4 px-6">Stato</th>
                <th className="py-4 px-6">Data Creazione</th>
                <th className="py-4 px-6 text-right">Azioni</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-xs font-mono">
              {sandboxKeys.map((key) => {
                const isRevealed = revealedKeys[key.id];
                const displaySecret = key.keySecret || `${key.keyPrefix}••••••••••••••••••••••••`;
                const isCopied = copiedKeyId === key.id;

                return (
                  <tr key={key.id} className="hover:bg-slate-900/40 transition-colors">
                    <td className="py-4 px-6 text-amber-400 font-bold">{key.keyPrefix}</td>
                    <td className="py-4 px-6 text-slate-300">
                      {isRevealed ? displaySecret : `${key.keyPrefix}••••••••••••••••••••••••`}
                    </td>
                    <td className="py-4 px-6 font-sans">
                      {key.revokedAt ? (
                        <span className="text-[10px] text-rose-400 font-semibold bg-rose-500/10 border border-rose-500/20 px-2.5 py-0.5 rounded-full">REVOCATA</span>
                      ) : (
                        <span className="text-[10px] text-emerald-400 font-semibold bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-0.5 rounded-full">ATTIVA</span>
                      )}
                    </td>
                    <td className="py-4 px-6 text-slate-400 font-sans">
                      {new Date(key.createdAt).toLocaleDateString()}
                    </td>
                    <td className="py-4 px-6 text-right font-sans space-x-2">
                      <button
                        onClick={() => toggleReveal(key.id)}
                        className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
                        title={isRevealed ? 'Nascondi' : 'Mostra'}
                      >
                        {isRevealed ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>

                      <button
                        onClick={() => handleCopy(key.id, displaySecret)}
                        className="p-1.5 rounded-lg bg-indigo-900/40 hover:bg-indigo-900/60 text-indigo-300 border border-indigo-700/50 transition-colors"
                        title="Copia negli appunti"
                      >
                        {isCopied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                      </button>

                      {!key.revokedAt && (
                        <button
                          onClick={() => handleRevoke(key.id)}
                          className="p-1.5 rounded-lg bg-rose-900/40 hover:bg-rose-900/60 text-rose-300 border border-rose-700/50 transition-colors"
                          title="Revoca Chiave"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Live Keys Section */}
      <div className="space-y-4 pt-4 border-t border-slate-800">
        <div className="flex items-center gap-2">
          <ShieldCheck className="w-5 h-5 text-emerald-400" />
          <h2 className="text-lg font-bold text-white">Chiavi API LIVE (Denaro Reale)</h2>
        </div>

        {merchant.status !== 'VERIFIED' ? (
          <div className="glass-panel p-8 text-center space-y-3 border border-amber-500/30">
            <ShieldAlert className="w-8 h-8 text-amber-400 mx-auto" />
            <h3 className="text-base font-bold text-white">Ambiente LIVE Bloccato</h3>
            <p className="text-xs text-slate-400 max-w-md mx-auto">
              Per accedere ed immettere chiavi API LIVE devi prima completare l'onboarding ed il form di verifica KYB.
            </p>
            <a href="#/onboarding" className="btn-primary inline-block text-xs font-semibold px-4 py-2 mt-2">
              Vai all'Onboarding & KYB →
            </a>
          </div>
        ) : (
          <div className="glass-panel overflow-hidden">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800 text-[11px] font-semibold text-slate-400 uppercase tracking-wider bg-slate-900/50">
                  <th className="py-4 px-6">Prefisso Chiave</th>
                  <th className="py-4 px-6">Secret / Masked Key</th>
                  <th className="py-4 px-6">Stato</th>
                  <th className="py-4 px-6">Data Creazione</th>
                  <th className="py-4 px-6 text-right">Azioni</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-xs font-mono">
                {liveKeys.map((key) => {
                  const isRevealed = revealedKeys[key.id];
                  const displaySecret = key.keySecret || `${key.keyPrefix}••••••••••••••••••••••••`;
                  const isCopied = copiedKeyId === key.id;

                  return (
                    <tr key={key.id} className="hover:bg-slate-900/40 transition-colors">
                      <td className="py-4 px-6 text-emerald-400 font-bold">{key.keyPrefix}</td>
                      <td className="py-4 px-6 text-slate-300">
                        {isRevealed ? displaySecret : `${key.keyPrefix}••••••••••••••••••••••••`}
                      </td>
                      <td className="py-4 px-6 font-sans">
                        {key.revokedAt ? (
                          <span className="text-[10px] text-rose-400 font-semibold bg-rose-500/10 border border-rose-500/20 px-2.5 py-0.5 rounded-full">REVOCATA</span>
                        ) : (
                          <span className="text-[10px] text-emerald-400 font-semibold bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-0.5 rounded-full">ATTIVA</span>
                        )}
                      </td>
                      <td className="py-4 px-6 text-slate-400 font-sans">
                        {new Date(key.createdAt).toLocaleDateString()}
                      </td>
                      <td className="py-4 px-6 text-right font-sans space-x-2">
                        <button
                          onClick={() => toggleReveal(key.id)}
                          className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
                          title={isRevealed ? 'Nascondi' : 'Mostra'}
                        >
                          {isRevealed ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                        </button>

                        <button
                          onClick={() => handleCopy(key.id, displaySecret)}
                          className="p-1.5 rounded-lg bg-indigo-900/40 hover:bg-indigo-900/60 text-indigo-300 border border-indigo-700/50 transition-colors"
                          title="Copia negli appunti"
                        >
                          {isCopied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                        </button>

                        {!key.revokedAt && (
                          <button
                            onClick={() => handleRevoke(key.id)}
                            className="p-1.5 rounded-lg bg-rose-900/40 hover:bg-rose-900/60 text-rose-300 border border-rose-700/50 transition-colors"
                            title="Revoca Chiave"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
