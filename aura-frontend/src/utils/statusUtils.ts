import type { PaymentStatus, KYBStatus, InvoiceStatus, DeliveryStatus } from '../types';

export interface StatusInfo {
  label: string;
  description: string;
  bgClass: string;
  textClass: string;
  borderClass: string;
  icon: string;
}

export const getPaymentStatusInfo = (status: PaymentStatus): StatusInfo => {
  switch (status) {
    case 'SUCCEEDED':
      return {
        label: 'Pagamento Confermato',
        description: 'L\'addebito è andato a buon fine ed i fondi sono stati accreditati nel mastro.',
        bgClass: 'bg-emerald-50',
        textClass: 'text-emerald-700',
        borderClass: 'border-emerald-200',
        icon: '✓',
      };
    case 'PROCESSING':
      return {
        label: 'In Autorizzazione',
        description: 'Transazione in fase di verifica presso l\'Acquiring Bank.',
        bgClass: 'bg-indigo-50',
        textClass: 'text-indigo-700',
        borderClass: 'border-indigo-200',
        icon: '⏳',
      };
    case 'FAILED':
      return {
        label: 'Transazione Rifiutata',
        description: 'L\'addebito è stato respinto dalla banca o dai controlli di sicurezza.',
        bgClass: 'bg-rose-50',
        textClass: 'text-rose-700',
        borderClass: 'border-rose-200',
        icon: '✕',
      };
    case 'CANCELLED':
      return {
        label: 'Annullata dall\'Utente',
        description: 'L\'operazione di pagamento è stata annullata prima dell\'autorizzazione.',
        bgClass: 'bg-zinc-100',
        textClass: 'text-zinc-700',
        borderClass: 'border-zinc-300',
        icon: '⊘',
      };
    case 'REFUNDED':
      return {
        label: 'Rimborsato Integrale',
        description: 'L\'importo totale è stato riaccreditato sulla carta del cliente.',
        bgClass: 'bg-purple-50',
        textClass: 'text-purple-700',
        borderClass: 'border-purple-200',
        icon: '↩',
      };
    case 'PARTIALLY_REFUNDED':
      return {
        label: 'Rimborsato Parziale',
        description: 'Una quota dell\'importo originario è stata rimborsata.',
        bgClass: 'bg-amber-50',
        textClass: 'text-amber-700',
        borderClass: 'border-amber-200',
        icon: '↩',
      };
    default:
      return {
        label: 'Inizializzata',
        description: 'Pagamento creato in attesa di confermazione.',
        bgClass: 'bg-zinc-100',
        textClass: 'text-zinc-600',
        borderClass: 'border-zinc-200',
        icon: '•',
      };
  }
};

export const getKYBStatusInfo = (status: KYBStatus): StatusInfo => {
  switch (status) {
    case 'VERIFIED':
      return {
        label: 'Esercente Verificato (Live Attivo)',
        description: 'Verifica fiscale KYB completata. L\'esercente può incassare in ambiente di produzione.',
        bgClass: 'bg-emerald-50',
        textClass: 'text-emerald-800',
        borderClass: 'border-emerald-300',
        icon: '🛡️',
      };
    case 'VERIFICATION_REJECTED':
      return {
        label: 'Verifica Fiscale Non Superata',
        description: 'I dati societari o la Partita IVA non hanno superato i controlli di conformità.',
        bgClass: 'bg-rose-50',
        textClass: 'text-rose-800',
        borderClass: 'border-rose-300',
        icon: '🔴',
      };
    case 'SUSPENDED':
      return {
        label: 'Account Sospeso',
        description: 'L\'esercente è stato temporaneamente sospeso per verifiche di conformità.',
        bgClass: 'bg-zinc-100',
        textClass: 'text-zinc-800',
        borderClass: 'border-zinc-300',
        icon: '⚠️',
      };
    default:
      return {
        label: 'In Attesa di Documenti (Sandbox Only)',
        description: 'Account in ambiente di prova. Completa la verifica KYB per attivare la produzione Live.',
        bgClass: 'bg-amber-50',
        textClass: 'text-amber-800',
        borderClass: 'border-amber-300',
        icon: '📋',
      };
  }
};

export const getInvoiceStatusInfo = (status: InvoiceStatus): StatusInfo => {
  switch (status) {
    case 'GENERATED':
      return {
        label: 'Documento Emesso',
        description: 'Fattura generata in PDF ed archiviata su MinIO S3.',
        bgClass: 'bg-emerald-50',
        textClass: 'text-emerald-700',
        borderClass: 'border-emerald-200',
        icon: '📄',
      };
    default:
      return {
        label: 'Errore Generazione PDF',
        description: 'Impossibile creare il documento fiscale.',
        bgClass: 'bg-rose-50',
        textClass: 'text-rose-700',
        borderClass: 'border-rose-200',
        icon: '⚠️',
      };
  }
};

export const getDeliveryStatusInfo = (status: DeliveryStatus): StatusInfo => {
  switch (status) {
    case 'SUCCESS':
      return {
        label: 'Inviato con Successo (HTTP 200)',
        description: 'Il server dell\'esercente ha confermato la ricezione del webhook.',
        bgClass: 'bg-emerald-50',
        textClass: 'text-emerald-700',
        borderClass: 'border-emerald-200',
        icon: '✓',
      };
    case 'DEAD_LETTER':
      return {
        label: 'Inoltrato a Coda Errore (DLQ)',
        description: 'Tutti i tentativi di invio sono falliti. Messaggio salvato nella Dead Letter Queue.',
        bgClass: 'bg-rose-50',
        textClass: 'text-rose-700',
        borderClass: 'border-rose-200',
        icon: '🔴',
      };
    default:
      return {
        label: 'In Corso di Invio',
        description: 'Chiamata HTTP in corso verso l\'endpoint dell\'esercente.',
        bgClass: 'bg-amber-50',
        textClass: 'text-amber-700',
        borderClass: 'border-amber-200',
        icon: '⏳',
      };
  }
};
