import type { 
  Merchant, 
  ApiKey, 
  PaymentIntent, 
  LedgerBalance, 
  LedgerEntry, 
  Invoice, 
  WebhookSubscription, 
  WebhookDelivery 
} from '../types';

const STORAGE_KEYS = {
  MERCHANT: 'aurapay_merchant',
  PAYMENTS: 'aurapay_payments',
  INVOICES: 'aurapay_invoices',
  BALANCES: 'aurapay_balances',
  WEBHOOK_SUBS: 'aurapay_webhook_subs',
  WEBHOOK_DELIVERIES: 'aurapay_webhook_deliveries',
  KAFKA_EVENTS: 'aurapay_kafka_events',
};

const loadFromStorage = <T>(key: string, fallback: T): T => {
  try {
    const data = localStorage.getItem(key);
    return data ? JSON.parse(data) : fallback;
  } catch (e) {
    return fallback;
  }
};

const saveToStorage = <T>(key: string, data: T): void => {
  try {
    localStorage.setItem(key, JSON.stringify(data));
  } catch (e) {
    console.error('Errore salvataggio localStorage:', e);
  }
};

export const mockMerchant: Merchant = loadFromStorage(STORAGE_KEYS.MERCHANT, {
  id: 'mch_acme_tech_2026',
  businessName: 'Acme Tech Solutions S.r.l.',
  vatNumber: 'IT12345678901',
  email: 'amministrazione@acmetech.it',
  status: 'VERIFIED',
  country: 'Italia',
  createdAt: '2026-07-01T10:00:00Z',
});

export const mockApiKeys: ApiKey[] = [
  {
    id: 'key_test_01',
    merchantId: mockMerchant.id,
    keyPrefix: 'sk_test_a1b2',
    environment: 'TEST',
    revokedAt: null,
    createdAt: '2026-07-01T10:05:00Z',
    keySecret: 'sk_test_demo_key_aurapay_2026_sandbox',
  },
  {
    id: 'key_live_01',
    merchantId: mockMerchant.id,
    keyPrefix: 'sk_live_99zz',
    environment: 'LIVE',
    revokedAt: null,
    createdAt: '2026-07-15T14:30:00Z',
    keySecret: 'sk_live_demo_key_aurapay_2026_production',
  },
];

const initialPayments: PaymentIntent[] = [
  {
    id: 'pi_89123456',
    merchantId: mockMerchant.id,
    amountCents: 12500,
    currency: 'EUR',
    status: 'SUCCEEDED',
    isTest: true,
    description: 'Abbonamento SaaS Enterprise - Piano Annuale',
    customerEmail: 'mario.rossi@azienda.it',
    authorizationCode: 'AUTH_891234',
    bankTransactionId: 'tx_bank_998123',
    createdAt: '2026-07-25T11:20:00Z',
    updatedAt: '2026-07-25T11:20:05Z',
  },
  {
    id: 'pi_77213499',
    merchantId: mockMerchant.id,
    amountCents: 4999,
    currency: 'EUR',
    status: 'PARTIALLY_REFUNDED',
    isTest: true,
    description: 'Licenza Modulo Addon Analytics',
    customerEmail: 'giulia.bianchi@studio.it',
    authorizationCode: 'AUTH_772134',
    bankTransactionId: 'tx_bank_441299',
    refundedAmountCents: 1000,
    createdAt: '2026-07-24T16:45:00Z',
    updatedAt: '2026-07-25T09:12:00Z',
  },
];

export const mockPayments: PaymentIntent[] = loadFromStorage(STORAGE_KEYS.PAYMENTS, initialPayments);

export const mockLedgerBalances: Record<'TEST' | 'LIVE', LedgerBalance> = loadFromStorage(STORAGE_KEYS.BALANCES, {
  TEST: {
    merchantId: mockMerchant.id,
    availableBalanceCents: 16499,
    currency: 'EUR',
    isTest: true,
  },
  LIVE: {
    merchantId: mockMerchant.id,
    availableBalanceCents: 35000,
    currency: 'EUR',
    isTest: false,
  },
});

export const mockLedgerEntries: LedgerEntry[] = [
  {
    id: 'led_01',
    merchantId: mockMerchant.id,
    paymentIntentId: 'pi_89123456',
    accountType: 'MERCHANT_AVAILABLE',
    entryType: 'CREDIT',
    amountCents: 12125,
    isTest: true,
    createdAt: '2026-07-25T11:20:05Z',
  },
];

const initialInvoices: Invoice[] = [
  {
    id: 'inv_2026_00101',
    invoiceNumber: 'INV-2026-000101',
    merchantId: mockMerchant.id,
    paymentIntentId: 'pi_89123456',
    amountCents: 12500,
    currency: 'EUR',
    invoiceType: 'INVOICE',
    status: 'GENERATED',
    isTest: true,
    pdfObjectKey: 'invoices/2026/mch_acme_tech/INV-2026-000101.pdf',
    createdAt: '2026-07-25T11:20:06Z',
  },
];

export const mockInvoices: Invoice[] = loadFromStorage(STORAGE_KEYS.INVOICES, initialInvoices);

const initialSubscriptions: WebhookSubscription[] = [
  {
    id: 'sub_01',
    merchantId: mockMerchant.id,
    targetUrl: 'https://api.acmetech.it/webhooks/aurapay',
    events: ['payment.succeeded', 'refund.succeeded', 'invoice.generated'],
    secretKey: 'whsec_demo_key_aurapay_2026_hmac',
    isTest: true,
    createdAt: '2026-07-05T09:00:00Z',
  },
];

export const mockWebhookSubscriptions: WebhookSubscription[] = loadFromStorage(STORAGE_KEYS.WEBHOOK_SUBS, initialSubscriptions);

const initialDeliveries: WebhookDelivery[] = [
  {
    id: 'del_01',
    subscriptionId: 'sub_01',
    eventId: 'evt_2b8f91cc',
    eventType: 'payment.succeeded',
    httpStatus: 200,
    attemptCount: 1,
    status: 'SUCCESS',
    lastError: null,
    nextAttemptAt: null,
    isTest: true,
    createdAt: '2026-07-25T11:20:06Z',
  },
];

export const mockWebhookDeliveries: WebhookDelivery[] = loadFromStorage(STORAGE_KEYS.WEBHOOK_DELIVERIES, initialDeliveries);

export interface KafkaEventRecord {
  id: string;
  topic: string;
  partition: number;
  offset: number;
  timestamp: string;
  producerService: string;
  payload: Record<string, any>;
}

const initialKafkaEvents: KafkaEventRecord[] = [
  {
    id: 'evt_9901a',
    topic: 'aura.payment.succeeded.v1',
    partition: 0,
    offset: 1042,
    timestamp: new Date().toISOString(),
    producerService: 'aura-payment-orchestrator',
    payload: {
      eventId: 'evt_9901a',
      eventType: 'aura.payment.succeeded.v1',
      paymentIntentId: 'pi_89123456',
      merchantId: 'mch_acme_tech_2026',
      amountCents: 12500,
      currency: 'EUR',
      authorizationCode: 'AUTH_891234',
      bankTransactionId: 'tx_bank_998123',
      isTest: true,
    },
  },
];

export const mockKafkaEvents: KafkaEventRecord[] = loadFromStorage(STORAGE_KEYS.KAFKA_EVENTS, initialKafkaEvents);

/**
 * Persiste ogni operazione in localStorage per resistere al ricaricamento (F5)
 */
export const syncStorage = () => {
  saveToStorage(STORAGE_KEYS.PAYMENTS, mockPayments);
  saveToStorage(STORAGE_KEYS.INVOICES, mockInvoices);
  saveToStorage(STORAGE_KEYS.BALANCES, mockLedgerBalances);
  saveToStorage(STORAGE_KEYS.WEBHOOK_SUBS, mockWebhookSubscriptions);
  saveToStorage(STORAGE_KEYS.WEBHOOK_DELIVERIES, mockWebhookDeliveries);
  saveToStorage(STORAGE_KEYS.KAFKA_EVENTS, mockKafkaEvents);
};

export const registerMockCompletedPayment = (payment: PaymentIntent) => {
  const existingIdx = mockPayments.findIndex((p) => p.id === payment.id);
  if (existingIdx >= 0) {
    mockPayments[existingIdx] = payment;
  } else {
    mockPayments.unshift(payment);
  }

  if (payment.status === 'SUCCEEDED') {
    const envKey = payment.isTest ? 'TEST' : 'LIVE';
    mockLedgerBalances[envKey].availableBalanceCents += payment.amountCents;

    const invNum = `INV-2026-${Math.floor(100000 + Math.random() * 900000)}`;
    const newInvoice: Invoice = {
      id: `inv_${payment.id}`,
      invoiceNumber: invNum,
      merchantId: payment.merchantId,
      paymentIntentId: payment.id,
      amountCents: payment.amountCents,
      currency: payment.currency,
      invoiceType: 'INVOICE',
      status: 'GENERATED',
      isTest: payment.isTest,
      pdfObjectKey: `invoices/2026/${payment.merchantId}/${invNum}.pdf`,
      createdAt: new Date().toISOString(),
    };
    mockInvoices.unshift(newInvoice);

    mockKafkaEvents.unshift({
      id: `evt_${payment.id}`,
      topic: 'aura.payment.succeeded.v1',
      partition: 0,
      offset: Math.floor(2000 + Math.random() * 5000),
      timestamp: new Date().toISOString(),
      producerService: 'aura-payment-orchestrator',
      payload: {
        eventId: `evt_${payment.id}`,
        eventType: 'aura.payment.succeeded.v1',
        paymentIntentId: payment.id,
        merchantId: payment.merchantId,
        amountCents: payment.amountCents,
        currency: payment.currency,
        authorizationCode: payment.authorizationCode || 'AUTH_DEMO',
        bankTransactionId: payment.bankTransactionId || 'tx_bank_demo',
        isTest: payment.isTest,
      },
    });
  }

  syncStorage();
};

export const registerMockWebhookSubscription = (sub: WebhookSubscription) => {
  mockWebhookSubscriptions.unshift(sub);
  syncStorage();
};
