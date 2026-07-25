import React, { useEffect, useState } from 'react';
import { useMerchant } from '../context/MerchantContext';
import { webhookApi } from '../api/webhookApi';
import type { WebhookSubscription, WebhookDelivery } from '../types';
import { 
  Webhook, 
  RotateCcw, 
  Eye, 
  EyeOff, 
  AlertOctagon, 
  CheckCircle2, 
  Clock, 
  Link, 
  Key
} from 'lucide-react';
import { toast } from 'sonner';

export const WebhooksPage: React.FC = () => {
  const { merchant, isTest } = useMerchant();
  const [subscriptions, setSubscriptions] = useState<WebhookSubscription[]>([]);
  const [deliveries, setDeliveries] = useState<WebhookDelivery[]>([]);
  const [revealedSecrets, setRevealedSecrets] = useState<Record<string, boolean>>({});

  // Form nuova subscription
  const [targetUrl, setTargetUrl] = useState<string>('https://api.merchant.com/webhooks/aurapay');
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [replayingId, setReplayingId] = useState<string | null>(null);

  const fetchData = async () => {
    try {
      const [subs, dels] = await Promise.all([
        webhookApi.getSubscriptions(merchant.id, isTest),
        webhookApi.getDeliveries()
      ]);
      setSubscriptions(subs);
      setDeliveries(dels);
    } catch (err) {
      toast.error('Errore nel recupero dei webhook');
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
      setSubscriptions(prev => [newSub, ...prev]);
      toast.success('Endpoint Webhook configurato con successo!');
    } catch (err) {
      toast.error('Errore nella creazione dell\'endpoint');
    } finally {
      setSubmitting(false);
    }
  };

  const handleReplay = async (deliveryId: string) => {
    setReplayingId(deliveryId);
    try {
      await webhookApi.replayDelivery(deliveryId);
      toast.success('Replay manuale inviato! Consegna eseguita con successo.');
      fetchData();
    } catch (err) {
      toast.error('Errore durante il re-invio manuale');
    } finally {
      setReplayingId(null);
    }
  };

  const toggleSecret = (id: string) => {
    setRevealedSecrets(prev => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Title */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
            Webhook & Consegne Asincrone
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            Configura le notifiche HTTP firmate con HMAC-SHA256 e gestisci il replay delle Dead Letter.
          </p>
        </div>
      </div>

      {/* Subscription Settings Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Form Configurazione Endpoint */}
        <div className="glass-panel p-6 space-y-4">
          <div className="flex items-center gap-3 pb-3 border-b border-slate-800">
            <Webhook className="w-5 h-5 text-indigo-400" />
            <h2 className="text-lg font-bold text-white">Nuovo Endpoint Webhook</h2>
          </div>

          <form onSubmit={handleCreateSubscription} className="space-y-4 text-xs">
            <div>
              <label className="block text-slate-300 font-semibold mb-1">Target URL (HTTPS)</label>
              <div className="relative">
                <input
                  type="url"
                  value={targetUrl}
                  onChange={(e) => setTargetUrl(e.target.value)}
                  className="glass-input w-full pl-9"
                  placeholder="https://vostrosito.it/api/webhooks"
                  required
                />
                <Link className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
              </div>
            </div>

            <div>
              <label className="block text-slate-300 font-semibold mb-1">Eventi Sottoscritti</label>
              <div className="space-y-1.5 p-3 rounded-xl bg-slate-950/60 border border-slate-800 text-[11px] text-slate-300">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                  <span>payment.succeeded</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                  <span>refund.succeeded</span>
                </div>
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                  <span>invoice.generated</span>
                </div>
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="btn-primary w-full text-xs font-semibold py-2.5"
            >
              {submitting ? 'Salvataggio...' : 'Registra Target URL Webhook'}
            </button>
          </form>
        </div>

        {/* Subscriptions Active Cards */}
        <div className="glass-panel p-6 lg:col-span-2 space-y-4">
          <h2 className="text-lg font-bold text-white pb-3 border-b border-slate-800">
            Endpoint Attivi ({subscriptions.length})
          </h2>

          <div className="space-y-3">
            {subscriptions.length === 0 ? (
              <div className="text-xs text-slate-500 py-8 text-center">Nessun webhook registrato</div>
            ) : (
              subscriptions.map((sub) => {
                const isRevealed = revealedSecrets[sub.id];
                return (
                  <div key={sub.id} className="glass-card p-4 space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2 font-mono text-xs text-indigo-400 font-bold">
                        <Link className="w-4 h-4 text-indigo-400" />
                        <span>{sub.targetUrl}</span>
                      </div>
                      <span className="text-[10px] text-emerald-400 font-semibold bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-0.5 rounded-full">
                        ACTIVE
                      </span>
                    </div>

                    <div className="flex items-center justify-between text-xs bg-slate-950/60 p-3 rounded-lg border border-slate-800">
                      <div className="flex items-center gap-2 font-mono">
                        <Key className="w-3.5 h-3.5 text-slate-500" />
                        <span className="text-slate-400">HMAC Secret:</span>
                        <span className="text-slate-200">
                          {isRevealed ? sub.secretKey : `${sub.secretKey.substring(0, 10)}••••••••••••`}
                        </span>
                      </div>
                      <button
                        onClick={() => toggleSecret(sub.id)}
                        className="text-slate-400 hover:text-white p-1"
                      >
                        {isRevealed ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
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
      <div className="space-y-4 pt-4 border-t border-slate-800">
        <h2 className="text-lg font-bold text-white">Registro Storico Consegne Webhook</h2>

        <div className="glass-panel overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800 text-[11px] font-semibold text-slate-400 uppercase tracking-wider bg-slate-900/50">
                  <th className="py-4 px-6">ID Evento</th>
                  <th className="py-4 px-6">Tipo Evento</th>
                  <th className="py-4 px-6">HTTP Code</th>
                  <th className="py-4 px-6">Tentativi</th>
                  <th className="py-4 px-6">Stato Delivery</th>
                  <th className="py-4 px-6">Data</th>
                  <th className="py-4 px-6 text-right">Replay Manuale</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-xs">
                {deliveries.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="py-12 text-center text-slate-500">
                      Nessun tentativo di consegna registrato.
                    </td>
                  </tr>
                ) : (
                  deliveries.map((del) => (
                    <tr key={del.id} className="hover:bg-slate-900/40 transition-colors">
                      <td className="py-4 px-6 font-mono text-indigo-400 font-medium">
                        {del.eventId}
                      </td>
                      <td className="py-4 px-6 font-semibold text-slate-200">
                        {del.eventType}
                      </td>
                      <td className="py-4 px-6 font-mono font-bold">
                        {del.httpStatus === 200 ? (
                          <span className="text-emerald-400">200 OK</span>
                        ) : del.httpStatus ? (
                          <span className="text-rose-400">{del.httpStatus} Error</span>
                        ) : (
                          <span className="text-slate-500">-</span>
                        )}
                      </td>
                      <td className="py-4 px-6 text-slate-400 font-mono">
                        {del.attemptCount} / 5
                      </td>
                      <td className="py-4 px-6">
                        {del.status === 'SUCCESS' ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                            <CheckCircle2 className="w-3 h-3" />
                            <span>SUCCESS</span>
                          </span>
                        ) : del.status === 'DEAD_LETTER' ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/20 text-rose-400 border border-rose-500/30">
                            <AlertOctagon className="w-3 h-3" />
                            <span>DEAD_LETTER</span>
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/20 text-amber-400 border border-amber-500/30">
                            <Clock className="w-3 h-3" />
                            <span>PENDING</span>
                          </span>
                        )}
                      </td>
                      <td className="py-4 px-6 text-slate-400">
                        {new Date(del.createdAt).toLocaleDateString()} {new Date(del.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </td>
                      <td className="py-4 px-6 text-right">
                        {del.status === 'DEAD_LETTER' ? (
                          <button
                            onClick={() => handleReplay(del.id)}
                            disabled={replayingId === del.id}
                            className="bg-purple-600/30 hover:bg-purple-600/50 text-purple-300 border border-purple-500/40 text-xs px-3 py-1.5 rounded-lg inline-flex items-center gap-1.5 transition-colors"
                          >
                            <RotateCcw className="w-3.5 h-3.5" />
                            <span>{replayingId === del.id ? 'Re-invio...' : 'Replay Manuale'}</span>
                          </button>
                        ) : (
                          <span className="text-[10px] text-slate-600 font-mono">Nessuna azione</span>
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
