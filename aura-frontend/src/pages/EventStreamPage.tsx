import React, { useEffect, useState, useCallback } from 'react';
import {
  Radio,
  Search,
  Copy,
  Check,
  Filter,
  Layers,
  RefreshCw,
} from 'lucide-react';
import { toast } from 'sonner';
import { apiClient } from '../api/client';

export interface KafkaEventRecord {
  id: string;
  topic: string;
  partition: number;
  offset: number;
  timestamp: string;
  producerService: string;
  payload: Record<string, any>;
}

// Tutti i topic definiti in EventType.java (aura-core-lib)
const ALL_TOPICS: string[] = [
  'aura.merchant.created.v1',
  'aura.merchant.verified.v1',
  'aura.merchant.verification_rejected.v1',
  'aura.apikey.created.v1',
  'aura.apikey.revoked.v1',
  'aura.paymentintent.created.v1',
  'aura.payment.processing.v1',
  'aura.payment.succeeded.v1',
  'aura.payment.failed.v1',
  'aura.refund.requested.v1',
  'aura.refund.succeeded.v1',
  'aura.refund.failed.v1',
  'aura.invoice.generated.v1',
  'aura.invoice.generation_failed.v1',
  'aura.webhook.delivery_succeeded.v1',
  'aura.webhook.delivery_dead_lettered.v1',
  'aura.ledger.entry_recorded.v1',
  'aura.bank.authorization_result.v1',
];

// Colori per categoria topic
const topicColor = (topic: string): string => {
  if (topic.startsWith('aura.payment')) return 'bg-indigo-950 text-indigo-300 border-indigo-800';
  if (topic.startsWith('aura.merchant')) return 'bg-purple-950 text-purple-300 border-purple-800';
  if (topic.startsWith('aura.invoice')) return 'bg-emerald-950 text-emerald-300 border-emerald-800';
  if (topic.startsWith('aura.ledger')) return 'bg-amber-950 text-amber-300 border-amber-800';
  if (topic.startsWith('aura.webhook')) return 'bg-cyan-950 text-cyan-300 border-cyan-800';
  if (topic.startsWith('aura.refund')) return 'bg-orange-950 text-orange-300 border-orange-800';
  if (topic.startsWith('aura.apikey')) return 'bg-rose-950 text-rose-300 border-rose-800';
  if (topic.startsWith('aura.bank')) return 'bg-zinc-800 text-zinc-300 border-zinc-700';
  return 'bg-zinc-900 text-zinc-400 border-zinc-700';
};

export const EventStreamPage: React.FC = () => {
  const [events, setEvents] = useState<KafkaEventRecord[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);
  const [topicFilter, setTopicFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [autoRefresh, setAutoRefresh] = useState<boolean>(true);
  const [lastRefreshed, setLastRefreshed] = useState<Date>(new Date());

  const fetchEvents = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    else setRefreshing(true);
    try {
      const response = await apiClient.get('/v1/events');
      const data = Array.isArray(response.data) ? response.data : [];
      // Ordina per timestamp decrescente (più recenti prima)
      data.sort((a: KafkaEventRecord, b: KafkaEventRecord) =>
        new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
      );
      setEvents(data);
      setLastRefreshed(new Date());
    } catch (err) {
      console.error('Errore nel recupero degli eventi Kafka dal backend:', err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  // Initial load
  useEffect(() => {
    fetchEvents(false);
  }, [fetchEvents]);

  // Auto-refresh ogni 5 secondi
  useEffect(() => {
    if (!autoRefresh) return;
    const interval = setInterval(() => fetchEvents(true), 5000);
    return () => clearInterval(interval);
  }, [autoRefresh, fetchEvents]);

  const handleManualRefresh = () => {
    fetchEvents(true);
    toast.success('Stream aggiornato');
  };

  const filteredEvents = events.filter((e) => {
    const matchesTopic = topicFilter === 'ALL' || e.topic === topicFilter;
    const matchesQuery =
      e.topic.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (e.producerService || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
      JSON.stringify(e.payload).toLowerCase().includes(searchQuery.toLowerCase());
    return matchesTopic && matchesQuery;
  });

  const copyPayload = (payload: Record<string, any>, id: string) => {
    navigator.clipboard.writeText(JSON.stringify(payload, null, 2));
    setCopiedId(id);
    toast.success('Payload JSON copiato!');
    setTimeout(() => setCopiedId(null), 2000);
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Title */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-zinc-200 pb-5">
        <div>
          <h1 className="text-xl font-bold text-zinc-900 tracking-tight flex items-center gap-2">
            <Radio className="w-5 h-5 text-indigo-600" />
            <span>Flusso Eventi Kafka in Tempo Reale</span>
          </h1>
          <p className="text-zinc-500 text-xs mt-0.5">
            Monitora l'emissione dei messaggi in streaming sui topic del cluster Apache Kafka.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setAutoRefresh(prev => !prev)}
            className={`flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded border transition-colors ${
              autoRefresh
                ? 'bg-emerald-50 text-emerald-800 border-emerald-300'
                : 'bg-zinc-100 text-zinc-700 border-zinc-200 hover:bg-zinc-200'
            }`}
          >
            <span className={`w-2 h-2 rounded-full ${autoRefresh ? 'bg-emerald-600 animate-pulse' : 'bg-zinc-400'}`} />
            {autoRefresh ? 'Streaming Attivo (5s)' : 'Pausa Streaming'}
          </button>
          <button
            onClick={handleManualRefresh}
            disabled={refreshing}
            className="bg-white border border-zinc-300 text-zinc-700 hover:bg-zinc-50 text-xs px-3 py-1.5 flex items-center gap-1.5 font-semibold rounded disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? 'animate-spin' : ''}`} />
            <span>Aggiorna Ora</span>
          </button>
        </div>
      </div>

      {/* Stats row (Light Theme High Contrast) */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
        {[
          { label: 'Totale eventi registrati', value: events.length, color: 'text-zinc-900' },
          { label: 'Topic attivi', value: [...new Set(events.map(e => e.topic))].length, color: 'text-indigo-600' },
          { label: 'Eventi filtrati', value: filteredEvents.length, color: 'text-emerald-700' },
          { label: 'Ultimo controllo', value: lastRefreshed.toLocaleTimeString('it-IT'), color: 'text-zinc-600' },
        ].map((stat) => (
          <div key={stat.label} className="p-3.5 rounded-lg bg-white border border-zinc-200 shadow-sm">
            <div className={`font-mono font-bold text-lg ${stat.color}`}>{stat.value}</div>
            <div className="text-zinc-500 text-[11px] mt-0.5">{stat.label}</div>
          </div>
        ))}
      </div>

      {/* Filter Bar */}
      <div className="p-4 rounded-lg bg-white border border-zinc-200 shadow-xs flex flex-col md:flex-row gap-4 justify-between items-center text-xs">
        <div className="input-with-icon-wrapper w-full md:w-80">
          <Search className="w-4 h-4 input-icon" />
          <input
            type="text"
            placeholder="Cerca per payload o ID evento..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="shadcn-input w-full text-xs"
          />
        </div>

        <div className="flex items-center gap-2 w-full md:w-auto">
          <Filter className="w-3.5 h-3.5 text-zinc-400 shrink-0" />
          <span className="text-zinc-500 font-medium">Topic Kafka:</span>
          <select
            value={topicFilter}
            onChange={(e) => setTopicFilter(e.target.value)}
            className="shadcn-input text-xs cursor-pointer"
          >
            <option value="ALL">Tutti i Topic Kafka ({ALL_TOPICS.length})</option>
            {ALL_TOPICS.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Events List (Light Theme) */}
      <div className="space-y-3">
        {loading ? (
          <div className="p-12 text-center text-zinc-400 text-xs bg-white rounded-lg border border-zinc-200">
            <Radio className="w-6 h-6 mx-auto mb-3 animate-pulse text-indigo-600" />
            Caricamento eventi dal cluster Kafka in corso...
          </div>
        ) : filteredEvents.length === 0 ? (
          <div className="p-12 text-center text-zinc-400 text-xs bg-white rounded-lg border border-zinc-200">
            <Layers className="w-6 h-6 mx-auto mb-3 text-zinc-400" />
            <p className="font-bold text-zinc-800 text-sm">Nessun evento registrato nel log</p>
            <p className="mt-1 text-zinc-500">Esegui un pagamento dal Simulatore di Checkout per osservare la cascata di eventi in tempo reale.</p>
          </div>
        ) : (
          filteredEvents.map((evt, idx) => (
            <div key={evt.id ? `stream-${evt.id}` : `stream-idx-${idx}`} className="p-4 rounded-lg bg-white border border-zinc-200 hover:border-zinc-300 shadow-xs transition-colors space-y-3 text-xs">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div className="flex items-center gap-2 flex-wrap">
                  <Layers className="w-4 h-4 text-indigo-600 shrink-0" />
                  <span className={`font-mono text-[11px] font-bold px-2 py-0.5 rounded border ${topicColor(evt.topic)}`}>
                    {evt.topic}
                  </span>
                  <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-zinc-100 border border-zinc-200 text-zinc-600 font-semibold">
                    Partizione:{evt.partition} Offset:{evt.offset}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-[11px] text-zinc-500 font-sans">
                    {new Date(evt.timestamp).toLocaleTimeString('it-IT')}
                  </span>
                  <button
                    onClick={() => copyPayload(evt.payload, evt.id)}
                    className="p-1.5 rounded bg-zinc-100 hover:bg-zinc-200 text-zinc-700 transition-colors flex items-center gap-1 text-[11px]"
                    title="Copia Payload JSON"
                  >
                    {copiedId === evt.id ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                    <span>{copiedId === evt.id ? 'Copiato!' : 'Copia'}</span>
                  </button>
                </div>
              </div>

              <div className="pt-2 border-t border-zinc-100">
                <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider block mb-1">Payload JSON Evento Kafka:</span>
                <pre className="p-3 rounded bg-zinc-900 text-amber-300 font-mono text-[11px] overflow-x-auto">
                  {JSON.stringify(evt.payload, null, 2)}
                </pre>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
