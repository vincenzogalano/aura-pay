import React, { useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { merchantApi } from '../api/merchantApi';
import type { ApiKey } from '../types';
import { 
  Copy, 
  Check, 
  Eye, 
  EyeOff, 
  Plus
} from 'lucide-react';
import { toast } from 'sonner';

export const ApiKeysPage: React.FC = () => {
  const { merchant, apiKeys, addApiKey, revokeApiKey } = useMerchant();
  const [visibleKeys, setVisibleKeys] = useState<Record<string, boolean>>({});
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [generating, setGenerating] = useState<boolean>(false);

  const toggleVisibility = (id: string) => {
    setVisibleKeys(prev => ({ ...prev, [id]: !prev[id] }));
  };

  const copyToClipboard = (keySecret: string, id: string) => {
    navigator.clipboard.writeText(keySecret);
    setCopiedId(id);
    toast.success('Chiave API copiata!');
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleGenerateTestKey = async () => {
    setGenerating(true);
    try {
      const newKeys = await merchantApi.generateTestKeys(merchant.id);
      if (Array.isArray(newKeys)) {
        newKeys.forEach(k => addApiKey(k));
      } else if (newKeys) {
        addApiKey(newKeys);
      }
      toast.success('Nuova coppia di API Keys TEST generata!');
    } catch (err) {
      toast.error('Errore durante la generazione della chiave TEST');
    } finally {
      setGenerating(false);
    }
  };

  const handleGenerateLiveKey = async () => {
    if (merchant.status !== 'VERIFIED') {
      toast.error('Devi completare la verifica KYB prima di generare chiavi LIVE!');
      return;
    }
    setGenerating(true);
    try {
      const newKeys = await merchantApi.generateLiveKeys(merchant.id);
      if (Array.isArray(newKeys)) {
        newKeys.forEach(k => addApiKey(k));
      } else if (newKeys) {
        addApiKey(newKeys);
      }
      toast.success('Nuova coppia di API Keys LIVE generata!');
    } catch (err) {
      toast.error('Errore durante la generazione della chiave LIVE');
    } finally {
      setGenerating(false);
    }
  };

  const handleRevoke = (id: string) => {
    revokeApiKey(id);
    toast.info('API Key revocata.');
  };

  return (
    <div className="space-y-6 max-w-5xl animate-fadeIn">
      {/* Title */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-zinc-800 pb-5">
        <div>
          <h1 className="text-xl font-bold text-zinc-100 tracking-tight">
            Gestione Chiavi API
          </h1>
          <p className="text-zinc-400 text-xs mt-0.5">
            Genera e gestisci le tue chiavi segrete TEST e LIVE per l'autenticazione delle richieste API Gateway.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleGenerateTestKey}
            disabled={generating}
            className="btn-shadcn-secondary text-xs font-semibold px-3 py-2 inline-flex items-center gap-1.5 disabled:opacity-50"
          >
            <Plus className="w-4 h-4 text-amber-400" />
            <span>Genera Chiavi TEST</span>
          </button>

          <button
            onClick={handleGenerateLiveKey}
            disabled={generating || merchant.status !== 'VERIFIED'}
            className="btn-shadcn-primary text-xs font-semibold px-3 py-2 inline-flex items-center gap-1.5 disabled:opacity-50"
          >
            <Plus className="w-4 h-4 text-emerald-400" />
            <span>{generating ? 'Generazione...' : 'Genera Chiavi LIVE'}</span>
          </button>
        </div>
      </div>

      {/* Keys Table (Shadcn style) */}
      <div className="rounded-lg border border-zinc-800 overflow-hidden bg-zinc-950">
        <table className="w-full text-left border-collapse text-xs">
          <thead>
            <tr className="border-b border-zinc-800 text-[11px] font-semibold text-zinc-400 uppercase tracking-wider bg-zinc-900/60">
              <th className="py-3 px-4">Ambiente</th>
              <th className="py-3 px-4">Tipo</th>
              <th className="py-3 px-4">Prefisso / Valore</th>
              <th className="py-3 px-4">Data Creazione</th>
              <th className="py-3 px-4 text-right">Azioni</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-800/60">
            {apiKeys.length === 0 ? (
              <tr>
                <td colSpan={5} className="py-8 text-center text-zinc-500">
                  Nessuna chiave API presente. Genera una chiave TEST o LIVE per iniziare.
                </td>
              </tr>
            ) : (
              apiKeys.map((key: ApiKey) => {
                const isVisible = !!visibleKeys[key.id];
                const displaySecret = isVisible ? (key.keySecret || key.rawKey || key.keyPrefix) : `${key.keyPrefix}_••••••••••••••••••••`;
                const isRevoked = !!key.revokedAt;

                return (
                  <tr key={key.id} className="hover:bg-zinc-900/40 transition-colors">
                    <td className="py-3.5 px-4">
                      <span className={`text-[10px] font-mono font-semibold px-2 py-0.5 rounded border ${
                        key.environment === 'TEST' ? 'bg-amber-950 text-amber-400 border-amber-800' : 'bg-emerald-950 text-emerald-400 border-emerald-800'
                      }`}>
                        {key.environment}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-zinc-400">
                      {key.keyType || (key.keyPrefix?.startsWith('pk') ? 'PUBLIC' : 'SECRET')}
                    </td>
                    <td className="py-3.5 px-4 font-mono text-zinc-200">
                      {isRevoked ? <span className="line-through text-zinc-600">{displaySecret}</span> : displaySecret}
                    </td>
                    <td className="py-3.5 px-4 text-zinc-400">
                      {key.createdAt ? new Date(key.createdAt).toLocaleDateString('it-IT') : 'Oggi'}
                    </td>
                    <td className="py-3.5 px-4 text-right space-x-2">
                      {!isRevoked && (
                        <>
                          <button
                            onClick={() => toggleVisibility(key.id)}
                            className="p-1.5 rounded bg-zinc-900 hover:bg-zinc-800 text-zinc-300"
                            title={isVisible ? 'Nascondi' : 'Mostra'}
                          >
                            {isVisible ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                          </button>

                          <button
                            onClick={() => copyToClipboard(key.keySecret || key.rawKey || key.keyPrefix, key.id)}
                            className="p-1.5 rounded bg-zinc-900 hover:bg-zinc-800 text-zinc-300"
                            title="Copia"
                          >
                            {copiedId === key.id ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                          </button>

                          <button
                            onClick={() => handleRevoke(key.id)}
                            className="px-2.5 py-1 rounded bg-rose-950/60 hover:bg-rose-900 text-rose-300 border border-rose-800 text-xs transition-colors"
                          >
                            Revoca
                          </button>
                        </>
                      )}
                      {isRevoked && <span className="text-xs text-rose-500 font-medium">Revocata</span>}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
