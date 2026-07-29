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
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-zinc-800 pb-5">
        <div>
          <h1 className="text-xl font-bold text-zinc-100 tracking-tight flex items-center gap-2">
            <Radio className="w-5 h-5 text-emerald-400" />
            <span>Stream Eventi Apache Kafka & Outbox</span>
          </h1>
          <p className="text-zinc-400 text-xs mt-0.5">
            Monitoraggio degli eventi pubblicati dal Transactional Outbox Pattern ({ALL_TOPICS.length} topic registrati).
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setAutoRefresh(v => !v)}
            className={`flex items-center gap-1.5 text-[11px] px-3 py-1.5 rounded border transition-colors ${
              autoRefresh
                ? 'bg-emerald-950 text-emerald-400 border-emerald-800'
                : 'bg-zinc-900 text-zinc-400 border-zinc-700 hover:border-zinc-600'
            }`}
          >
            <span className={`w-1.5 h-1.5 rounded-full ${autoRefresh ? 'bg-emerald-400 animate-pulse' : 'bg-zinc-600'}`} />
            {autoRefresh ? 'Live (5s)' : 'Paused'}
          </button>
          <button
            onClick={handleManualRefresh}
            disabled={refreshing}
            className="flex items-center gap-1.5 text-[11px] px-3 py-1.5 rounded border bg-zinc-900 border-zinc-700 hover:border-zinc-600 text-zinc-400 hover:text-zinc-100 transition-colors disabled:opacity-50"
          >
            <RefreshCw className={`w-3 h-3 ${refreshing ? 'animate-spin' : ''}`} />
            Aggiorna
          </button>
        </div>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
        {[
          { label: 'Totale eventi', value: events.length, color: 'text-zinc-100' },
          { label: 'Topic attivi', value: [...new Set(events.map(e => e.topic))].length, color: 'text-indigo-400' },
          { label: 'Filtrati', value: filteredEvents.length, color: 'text-emerald-400' },
          { label: 'Ultimo aggiornamento', value: lastRefreshed.toLocaleTimeString(), color: 'text-zinc-400' },
        ].map((stat) => (
          <div key={stat.label} className="p-3 rounded-lg bg-zinc-900/60 border border-zinc-800">
            <div className={`font-mono font-bold text-base ${stat.color}`}>{stat.value}</div>
            <div className="text-zinc-500 mt-0.5">{stat.label}</div>
          </div>
        ))}
      </div>

      {/* Filter Bar */}
      <div className="p-4 rounded-lg bg-zinc-900/60 border border-zinc-800 flex flex-col md:flex-row gap-4 justify-between items-center text-xs">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-zinc-400 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Cerca per topic, payload o servizio..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="shadcn-input pl-9 w-full text-xs"
          />
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto justify-end">
          <div className="flex items-center gap-1.5 text-zinc-500">
            <Filter className="w-3.5 h-3.5" />
            <span>Topic:</span>
          </div>
          <select
            value={topicFilter}
            onChange={(e) => setTopicFilter(e.target.value)}
            className="shadcn-input text-xs cursor-pointer"
          >
            <option value="ALL">Tutti i Topic ({ALL_TOPICS.length})</option>
            {ALL_TOPICS.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Events List */}
      <div className="space-y-3">
        {loading ? (
          <div className="p-12 text-center text-zinc-400 text-xs bg-zinc-900/40 rounded-lg border border-zinc-800">
            <Radio className="w-6 h-6 mx-auto mb-3 animate-pulse text-zinc-600" />
            Caricamento eventi dal cluster Kafka...
          </div>
        ) : filteredEvents.length === 0 ? (
          <div className="p-12 text-center text-zinc-500 text-xs bg-zinc-900/40 rounded-lg border border-zinc-800">
            <Layers className="w-6 h-6 mx-auto mb-3 text-zinc-700" />
            <p className="font-medium text-zinc-400">Nessun evento nel log</p>
            <p className="mt-1">Esegui un pagamento dal Checkout Demo per vedere gli eventi in tempo reale.</p>
          </div>
        ) : (
          filteredEvents.map((evt) => (
            <div key={evt.id} className="p-4 rounded-lg bg-zinc-900/60 border border-zinc-800 hover:border-zinc-700 transition-colors space-y-3">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div className="flex items-center gap-2 flex-wrap">
                  <Layers className="w-4 h-4 text-zinc-500 flex-shrink-0" />
                  <span className={`font-mono text-[11px] font-semibold px-2 py-0.5 rounded border ${topicColor(evt.topic)}`}>
                    {evt.topic}
                  </span>
                  <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-zinc-800 border border-zinc-700 text-zinc-400">
                    P:{evt.partition} O:{evt.offset}
                  </span>
                </div>
                <div className="text-[11px] font-mono text-zinc-500 flex-shrink-0">
                  <span className="text-zinc-600">by</span>{' '}
                  <span className="text-zinc-400">{evt.producerService}</span>{' '}
                  <span className="text-zinc-600">•</span>{' '}
                  {new Date(evt.timestamp).toLocaleTimeString()}
                </div>
              </div>

              <div className="relative">
                <button
                  onClick={() => copyPayload(evt.payload, evt.id)}
                  className="absolute right-3 top-3 text-zinc-500 hover:text-zinc-200 p-1 flex items-center gap-1 text-[10px] z-10 transition-colors"
                >
                  {copiedId === evt.id ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                  <span>{copiedId === evt.id ? 'Copiato!' : 'Copia'}</span>
                </button>
                <pre className="p-4 rounded-md bg-zinc-950 text-zinc-300 font-mono text-[11px] overflow-x-auto max-h-64 border border-zinc-800">
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
