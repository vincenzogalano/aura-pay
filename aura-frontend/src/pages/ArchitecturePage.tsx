import React, { useState } from 'react';
import { 
  Network, 
  Server, 
  Radio, 
  Cpu, 
  Zap,
  HelpCircle
} from 'lucide-react';
import { toast } from 'sonner';

interface ServiceNode {
  id: string;
  name: string;
  port: number;
  type: 'GATEWAY' | 'CORE' | 'SECURITY' | 'INTEGRATION' | 'STORAGE';
  description: string;
  tech: string;
  producedEvents: string[];
  consumedEvents: string[];
}

export const ArchitecturePage: React.FC = () => {
  const [selectedService, setSelectedService] = useState<string | null>('payment-orchestrator');
  const [simulatingFlow, setSimulatingFlow] = useState<boolean>(false);
  const [activeKafkaTopic, setActiveKafkaTopic] = useState<string | null>(null);

  const services: ServiceNode[] = [
    {
      id: 'api-gateway',
      name: 'aura-api-gateway',
      port: 8080,
      type: 'GATEWAY',
      description: 'Single Entry Point con Spring Cloud Gateway, rate limiting su Redis e routing dinamico verso i microservizi.',
      tech: 'Spring Cloud Gateway, Redis, Netty',
      producedEvents: [],
      consumedEvents: [],
    },
    {
      id: 'merchant-service',
      name: 'aura-merchant-service',
      port: 8081,
      type: 'CORE',
      description: 'Gestione del profilo societario esercente, gestione API Keys Sandbox/Live e motore di verifica fiscalità KYB.',
      tech: 'Spring Boot 3, PostgreSQL, Kafka Producer',
      producedEvents: ['aura.merchant.created.v1', 'aura.merchant.verified.v1', 'aura.merchant.verification_rejected.v1'],
      consumedEvents: [],
    },
    {
      id: 'payment-orchestrator',
      name: 'aura-payment-orchestrator',
      port: 8082,
      type: 'CORE',
      description: 'Orchestratore delle transazioni di pagamento. Applica il Transactional Outbox Pattern su DB per garantire consistency atomica con Kafka.',
      tech: 'Spring Boot 3, PostgreSQL, Transactional Outbox Pattern',
      producedEvents: ['aura.payment.created.v1', 'aura.payment.succeeded.v1', 'aura.payment.failed.v1', 'aura.refund.requested.v1', 'aura.refund.failed.v1'],
      consumedEvents: ['aura.merchant.verified.v1'],
    },
    {
      id: 'vault-service',
      name: 'aura-vault-service',
      port: 8084,
      type: 'SECURITY',
      description: 'Microservizio isolato scope PCI-DSS per la tokenizzazione dei dati di carta di credito con cifratura AES-256 via HashiCorp Vault Transit Engine.',
      tech: 'Spring Boot 3, HashiCorp Vault API, PCI-DSS Compliant',
      producedEvents: [],
      consumedEvents: [],
    },
    {
      id: 'ledger-service',
      name: 'aura-ledger-service',
      port: 8085,
      type: 'CORE',
      description: 'Microservizio contabile ad immutabilità garantita. Registra la Partita Doppia (DARE/AVERE) per ciascuna transazione consumata da Kafka.',
      tech: 'Spring Boot 3, PostgreSQL, Kafka Consumer',
      producedEvents: ['aura.ledger.entry_created.v1', 'aura.ledger.balance_updated.v1'],
      consumedEvents: ['aura.payment.succeeded.v1', 'aura.refund.succeeded.v1'],
    },
    {
      id: 'bank-simulator',
      name: 'aura-bank-simulator',
      port: 8086,
      type: 'INTEGRATION',
      description: 'Simulatore dell\'Acquiring Bank e dei circuiti di carte di credito (VISA, Mastercard) con supporto ad autorizzazioni Luhn e codici di rifiuto.',
      tech: 'Spring Boot 3, WireMock / Custom Bank API',
      producedEvents: [],
      consumedEvents: [],
    },
    {
      id: 'webhook-service',
      name: 'aura-webhook-service',
      port: 8087,
      type: 'INTEGRATION',
      description: 'Motore di notifica eventi agli endpoint degli esercenti con firma crittografica HMAC SHA-256 e gestione dei tentativi errati in Dead Letter Queue (DLQ).',
      tech: 'Spring Boot 3, PostgreSQL, HMAC SHA-256, DLQ Retry Engine',
      producedEvents: ['aura.webhook.delivered.v1', 'aura.webhook.delivery_failed.v1', 'aura.webhook.delivery_dead_lettered.v1'],
      consumedEvents: ['aura.payment.succeeded.v1', 'aura.payment.failed.v1', 'aura.refund.succeeded.v1', 'aura.invoice.generated.v1'],
    },
    {
      id: 'invoice-service',
      name: 'aura-invoice-service',
      port: 8088,
      type: 'STORAGE',
      description: 'Generatore asincrono di fatture e note di credito in formato PDF. Archivia i file su MinIO S3 ed emette URL di download pre-firmati temporanei.',
      tech: 'Spring Boot 3, OpenPDF, MinIO S3 Object Storage',
      producedEvents: ['aura.invoice.generated.v1', 'aura.invoice.generation_failed.v1'],
      consumedEvents: ['aura.payment.succeeded.v1', 'aura.refund.succeeded.v1'],
    },
  ];

  const handleSimulateCascade = () => {
    setSimulatingFlow(true);
    setActiveKafkaTopic('aura.payment.succeeded.v1');
    toast.info('Simulazione Flusso Event Driven avviata!');

    setTimeout(() => {
      setActiveKafkaTopic('aura.ledger.entry_created.v1');
    }, 1500);

    setTimeout(() => {
      setActiveKafkaTopic('aura.invoice.generated.v1');
    }, 3000);

    setTimeout(() => {
      setActiveKafkaTopic('aura.webhook.delivered.v1');
    }, 4500);

    setTimeout(() => {
      setSimulatingFlow(false);
      setActiveKafkaTopic(null);
      toast.success('Flusso ad eventi completato su tutti i microservizi!');
    }, 6000);
  };

  const selectedNode = services.find((s) => s.id === selectedService) || services[2];

  return (
    <div className="space-y-6 max-w-6xl animate-fadeIn">
      {/* Page Title */}
      <div className="border-b border-zinc-200 pb-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-zinc-900 tracking-tight flex items-center gap-2">
            <Network className="w-5 h-5 text-indigo-600" />
            <span>Mappa Architettura Microservizi &amp; Pipeline Kafka</span>
          </h1>
          <p className="text-zinc-500 text-xs mt-0.5">
            Topologia visiva degli 8 microservizi coordinati ed ispezione del flusso di notifiche in tempo reale.
          </p>
        </div>

        <button
          onClick={handleSimulateCascade}
          disabled={simulatingFlow}
          className="btn-shadcn-primary text-xs px-3 py-1.5 flex items-center gap-1.5 self-start md:self-auto"
        >
          <Zap className={`w-3.5 h-3.5 ${simulatingFlow ? 'animate-pulse text-amber-300' : ''}`} />
          <span>{simulatingFlow ? 'Simulazione Flusso in corso...' : '⚡ Simula Cascata Eventi Completa'}</span>
        </button>
      </div>

      {/* Guida Sezione Architettura */}
      <div className="p-4 rounded-lg bg-indigo-50 border border-indigo-200 text-indigo-900 text-xs space-y-1.5">
        <div className="flex items-center gap-2 font-bold text-indigo-950">
          <HelpCircle className="w-4 h-4 text-indigo-600 shrink-0" />
          <span>Come funziona l'architettura a Microservizi ad Eventi di AuraPay</span>
        </div>
        <p className="text-[11px] text-indigo-900 leading-relaxed">
          La piattaforma AuraPay è composta da <strong>8 microservizi autonomi</strong> che non si bloccano mai a vicenda. Quando un pagamento viene autorizzato, il sistema emette un evento sul broker Kafka che attiva contemporaneamente la scrittura nel <strong>Mastro Contabile</strong>, la generazione del <strong>PDF di Fattura</strong> e l'invio della <strong>Notifica Webhook</strong> al commerciante.
        </p>
      </div>

      {/* Interactive Microservices Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {services.map((service) => {
          const isSelected = service.id === selectedService;
          const isTargetedByActiveTopic = activeKafkaTopic && service.consumedEvents.includes(activeKafkaTopic);
          const isProducerOfActiveTopic = activeKafkaTopic && service.producedEvents.includes(activeKafkaTopic);

          return (
            <div
              key={service.id}
              onClick={() => setSelectedService(service.id)}
              className={`p-4 rounded-lg border cursor-pointer transition-all ${
                isSelected
                  ? 'bg-zinc-900 text-white border-zinc-900 shadow-md ring-2 ring-indigo-500'
                  : isProducerOfActiveTopic
                  ? 'bg-amber-50 border-amber-400 ring-2 ring-amber-400'
                  : isTargetedByActiveTopic
                  ? 'bg-emerald-50 border-emerald-400 ring-2 ring-emerald-400'
                  : 'bg-white border-zinc-200 hover:border-zinc-300 text-zinc-900'
              }`}
            >
              <div className="flex items-center justify-between mb-2">
                <span className={`text-[10px] font-mono font-semibold px-2 py-0.5 rounded border ${
                  isSelected 
                    ? 'bg-zinc-800 text-zinc-200 border-zinc-700'
                    : 'bg-zinc-100 text-zinc-600 border-zinc-200'
                }`}>
                  :{service.port}
                </span>
                <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded uppercase ${
                  service.type === 'GATEWAY' ? 'bg-indigo-100 text-indigo-700' :
                  service.type === 'SECURITY' ? 'bg-rose-100 text-rose-700' :
                  service.type === 'CORE' ? 'bg-emerald-100 text-emerald-700' : 'bg-zinc-100 text-zinc-700'
                }`}>
                  {service.type}
                </span>
              </div>

              <h3 className="font-bold text-xs font-mono tracking-tight mb-1">{service.name}</h3>
              <p className={`text-[11px] line-clamp-2 ${isSelected ? 'text-zinc-300' : 'text-zinc-500'}`}>
                {service.description}
              </p>
            </div>
          );
        })}
      </div>

      {/* Selected Microservice Deep-Dive */}
      <div className="p-5 rounded-lg bg-white border border-zinc-200 shadow-xs space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-zinc-100">
          <div className="flex items-center gap-2">
            <Server className="w-5 h-5 text-indigo-600" />
            <h2 className="text-sm font-bold text-zinc-900 font-mono">
              {selectedNode.name} <span className="text-zinc-400 font-sans text-xs font-normal">(Porta :{selectedNode.port})</span>
            </h2>
          </div>
          <span className="text-xs font-mono bg-zinc-100 px-2.5 py-1 rounded text-zinc-700 font-medium">
            Tech: {selectedNode.tech}
          </span>
        </div>

        <p className="text-xs text-zinc-600 leading-relaxed">
          {selectedNode.description}
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
          {/* Produced Events */}
          <div className="p-3.5 rounded-lg bg-zinc-50 border border-zinc-200 space-y-2">
            <span className="text-xs font-bold text-zinc-800 flex items-center gap-1.5">
              <Radio className="w-4 h-4 text-emerald-600" />
              <span>Eventi Kafka Pubblicati (Producers)</span>
            </span>
            {selectedNode.producedEvents.length === 0 ? (
              <span className="text-[11px] text-zinc-400 italic block">Nessun evento emesso.</span>
            ) : (
              <div className="space-y-1">
                {selectedNode.producedEvents.map((evt) => (
                  <span key={evt} className="block text-[11px] font-mono bg-white px-2 py-1 rounded border border-zinc-200 text-emerald-700 font-semibold">
                    {evt}
                  </span>
                ))}
              </div>
            )}
          </div>

          {/* Consumed Events */}
          <div className="p-3.5 rounded-lg bg-zinc-50 border border-zinc-200 space-y-2">
            <span className="text-xs font-bold text-zinc-800 flex items-center gap-1.5">
              <Cpu className="w-4 h-4 text-indigo-600" />
              <span>Eventi Kafka Consumati (Consumers)</span>
            </span>
            {selectedNode.consumedEvents.length === 0 ? (
              <span className="text-[11px] text-zinc-400 italic block">Nessun evento consumato.</span>
            ) : (
              <div className="space-y-1">
                {selectedNode.consumedEvents.map((evt) => (
                  <span key={evt} className="block text-[11px] font-mono bg-white px-2 py-1 rounded border border-zinc-200 text-indigo-700 font-semibold">
                    {evt}
                  </span>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Kafka Topic Live Pipeline Status */}
      {activeKafkaTopic && (
        <div className="p-4 rounded-lg bg-amber-950 border border-amber-800 text-amber-200 text-xs flex items-center justify-between animate-pulse">
          <div className="flex items-center gap-2">
            <Radio className="w-4 h-4 text-amber-400" />
            <span>Tracciamento Evento Kafka in corso: <strong className="font-mono text-amber-300">{activeKafkaTopic}</strong></span>
          </div>
          <span className="font-mono text-[10px] bg-amber-900 text-amber-200 px-2 py-0.5 rounded font-bold">LIVESTREAM</span>
        </div>
      )}
    </div>
  );
};
