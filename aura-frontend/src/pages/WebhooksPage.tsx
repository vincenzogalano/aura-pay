import React, { useEffect, useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { webhookApi } from '../api/webhookApi';
import type { WebhookSubscription, WebhookDelivery } from '../types';
import { 
  Webhook, 
  RotateCcw, 
  Eye, 
  EyeOff, 
  CheckCircle2, 
  Key,
  Trash2
} from 'lucide-react';
import { toast } from 'sonner';

export const WebhooksPage: React.FC = () => {
  const { merchant, isTest } = useMerchant();
  const [subscriptions, setSubscriptions] = useState<WebhookSubscription[]>([]);
  const [deliveries, setDeliveries] = useState<WebhookDelivery[]>([]);
  const [revealedSecrets, setRevealedSecrets] = useState<Record<string, boolean>>({});

  const [targetUrl, setTargetUrl] = useState<string>('https://webhook.site/32afb9ea-d829-484b-935c-664d5e1c020d');
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [replayingId, setReplayingId] = useState<string | null>(null);

  const fetchData = async () => {
    try {
      const [subs, dels] = await Promise.all([
        webhookApi.getSubscriptions(merchant.id, isTest),
        webhookApi.getDeliveries()
      ]);
      setSubscriptions(Array.isArray(subs) ? subs : []);
      setDeliveries(Array.isArray(dels) ? dels : []);
    } catch (err) {
      toast.error('Errore nel recupero dei webhook');
      setSubscriptions([]);
      setDeliveries([]);
    }
  };

  useEffect(() => {
    fetchData();
  }, [merchant.id, isTest]);

  const handleCreateSubscription = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const newSub = await webhookApi.createSubscription({
        merchantId: merchant.id,
        targetUrl,
        events: ['payment.succeeded', 'refund.succeeded', 'invoice.generated'],
        isTest,
      });
      setSubscriptions(prev => [newSub, ...prev.filter(s => s.id !== newSub.id)]);
      toast.success('Endpoint Webhook salvato!');
    } catch (err) {
      toast.error('Errore durante il salvataggio');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteSubscription = async (id: string) => {
    if (!confirm('Sei sicuro di voler eliminare questo endpoint webhook?')) return;
    try {
      await webhookApi.deleteSubscription(id);
      setSubscriptions(prev => prev.filter(s => s.id !== id));
      toast.success('Endpoint Webhook eliminato!');
    } catch (err) {
      toast.error('Errore durante l\'eliminazione del webhook');
    }
  };

  const handleReplay = async (deliveryId: string) => {
    setReplayingId(deliveryId);
    try {
      await webhookApi.replayDelivery(deliveryId);
      toast.success('Replay manuale eseguito!');
      fetchData();
    } catch (err) {
      toast.error('Errore durante il re-invio');
    } finally {
      setReplayingId(null);
    }
  };

  const toggleSecret = (id: string) => {
    setRevealedSecrets(prev => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Title */}
      <div className="border-b border-zinc-200 pb-5">
        <h1 className="text-xl font-bold text-zinc-900 tracking-tight">
          Notifiche Webhook E-Commerce
        </h1>
        <p className="text-zinc-500 text-xs mt-0.5">
          Configura gli URL del tuo server e-commerce dove AuraPay invierà notifiche HTTP POST trasparenti per aggiornare lo stato degli ordini.
        </p>
      </div>

      {/* Subscription Settings Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Form Configurazione Endpoint */}
        <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs space-y-4">
          <div className="flex items-center gap-2 pb-3 border-b border-zinc-100 text-xs font-bold text-zinc-900">
            <Webhook className="w-4 h-4 text-zinc-500" />
            <span>Configura URL Notifiche (Endpoint)</span>
          </div>

          <form onSubmit={handleCreateSubscription} className="space-y-3.5 text-xs">
            <div>
              <label className="block text-zinc-700 font-medium mb-1">Target URL Server E-Commerce (HTTPS)</label>
              <input
                type="url"
                value={targetUrl}
                onChange={(e) => setTargetUrl(e.target.value)}
                className="shadcn-input w-full font-mono text-[11px]"
                placeholder="https://vostrosito.it/api/webhooks"
                required
              />
            </div>

            <div>
              <label className="block text-zinc-700 font-medium mb-1">Eventi Notificati</label>
              <div className="space-y-1 p-2.5 rounded bg-zinc-50 border border-zinc-200 text-[11px] text-zinc-600">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                  <span>payment.succeeded (Pagamento approvato)</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                  <span>refund.succeeded (Storno/Rimborso)</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                  <span>invoice.generated (Fattura PDF MinIO)</span>
                </div>
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="btn-shadcn-primary w-full text-xs py-2 mt-2"
            >
              {submitting ? 'Salvataggio...' : 'Salva URL Notifica Webhook'}
            </button>
          </form>
        </div>

        {/* Subscriptions Active Cards */}
        <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs lg:col-span-2 space-y-4">
          <h2 className="text-xs font-bold text-zinc-900 pb-3 border-b border-zinc-100 uppercase tracking-wider">
            Endpoint Registrati ({subscriptions.length})
          </h2>

          <div className="space-y-3">
            {(Array.isArray(subscriptions) ? subscriptions : []).length === 0 ? (
              <div className="text-xs text-zinc-400 py-6 text-center">Nessun webhook configurato</div>
            ) : (
              (Array.isArray(subscriptions) ? subscriptions : []).map((sub) => {
                const isRevealed = revealedSecrets[sub.id];
                return (
                  <div key={sub.id} className="p-3.5 rounded bg-zinc-50 border border-zinc-200 space-y-2.5 text-xs">
                    <div className="flex items-center justify-between">
                      <div className="font-mono font-medium text-zinc-900 break-all pr-2">{sub.targetUrl}</div>
                      <div className="flex items-center gap-2 shrink-0">
                        <span className="text-[9px] font-mono font-semibold px-2 py-0.5 rounded border bg-emerald-50 text-emerald-700 border-emerald-200">
                          ATTIVO
                        </span>
                        <button
                          onClick={() => handleDeleteSubscription(sub.id)}
                          className="p-1 text-zinc-400 hover:text-rose-600 hover:bg-rose-50 rounded transition-colors"
                          title="Elimina Endpoint Webhook"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>

                    <div className="flex items-center justify-between text-xs bg-white p-2.5 rounded border border-zinc-200">
                      <div className="flex items-center gap-2 font-mono overflow-hidden">
                        <Key className="w-3.5 h-3.5 text-zinc-400 shrink-0" />
                        <span className="text-zinc-500 shrink-0">HMAC Secret:</span>
                        <span className="text-zinc-900 font-medium truncate">
                          {isRevealed ? sub.secretKey : `${sub.secretKey.substring(0, 10)}••••••••••••`}
                        </span>
                      </div>
                      <button
                        onClick={() => toggleSecret(sub.id)}
                        className="text-zinc-400 hover:text-zinc-900 p-1 shrink-0 ml-2"
                      >
                        {isRevealed ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                      </button>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>

      {/* Deliveries History Log */}
      <div className="space-y-3 pt-4 border-t border-zinc-200">
        <h2 className="text-sm font-bold text-zinc-900">Registro Consegne Webhook</h2>

        <div className="rounded-lg border border-zinc-200 overflow-hidden bg-white shadow-xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-zinc-200 text-[11px] font-semibold text-zinc-500 uppercase tracking-wider bg-zinc-50">
                  <th className="py-3 px-4">ID Evento</th>
                  <th className="py-3 px-4">Tipo Evento</th>
                  <th className="py-3 px-4">Stato HTTP</th>
                  <th className="py-3 px-4">Tentativi</th>
                  <th className="py-3 px-4">Stato Consegna</th>
                  <th className="py-3 px-4">Data</th>
                  <th className="py-3 px-4 text-right">Azione</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100 font-mono">
                {(Array.isArray(deliveries) ? deliveries : []).length === 0 ? (
                  <tr>
                    <td colSpan={7} className="py-8 text-center text-zinc-400 font-sans">
                      Nessun tentativo di consegna.
                    </td>
                  </tr>
                ) : (
                  (Array.isArray(deliveries) ? deliveries : []).map((del) => (
                    <tr key={del.id} className="hover:bg-zinc-50 transition-colors">
                      <td className="py-3 px-4 font-semibold text-zinc-900">{del.eventId}</td>
                      <td className="py-3 px-4 text-zinc-700 font-sans">{del.eventType}</td>
                      <td className="py-3 px-4 font-semibold">
                        {del.httpStatus === 200 ? (
                          <span className="text-emerald-600">200 OK</span>
                        ) : del.httpStatus ? (
                          <span className="text-rose-600">{del.httpStatus} Err</span>
                        ) : (
                          <span className="text-zinc-400">-</span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-zinc-500">{del.attemptCount} / 5</td>
                      <td className="py-3 px-4 font-sans">
                        <span className={`text-[10px] font-mono font-semibold px-2 py-0.5 rounded border ${
                          del.status === 'SUCCESS' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' :
                          del.status === 'DEAD_LETTER' ? 'bg-rose-50 text-rose-700 border-rose-200' :
                          'bg-amber-50 text-amber-700 border-amber-200'
                        }`}>
                          {del.status}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-zinc-500 font-sans">
                        {new Date(del.createdAt).toLocaleDateString('it-IT')}
                      </td>
                      <td className="py-3 px-4 text-right font-sans">
                        {del.status === 'DEAD_LETTER' ? (
                          <button
                            onClick={() => handleReplay(del.id)}
                            disabled={replayingId === del.id}
                            className="btn-shadcn-secondary text-xs py-1 px-2.5 inline-flex items-center gap-1"
                          >
                            <RotateCcw className="w-3 h-3" />
                            <span>{replayingId === del.id ? 'Replay...' : 'Replay Manuale'}</span>
                          </button>
                        ) : (
                          <span className="text-[10px] text-zinc-400 font-mono">-</span>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};
