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

export const mockMerchant: Merchant = {
  id: 'mch_acme_tech_2026',
  businessName: 'Acme Tech Solutions S.r.l.',
  vatNumber: 'IT12345678901',
  email: 'amministrazione@acmetech.it',
  status: 'VERIFIED',
  country: 'Italia',
  createdAt: '2026-07-01T10:00:00Z',
};

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
    id: 'key_test_pk',
    merchantId: mockMerchant.id,
    keyPrefix: 'pk_test_x9y8',
    environment: 'TEST',
    revokedAt: null,
    createdAt: '2026-07-01T10:05:00Z',
    keySecret: 'pk_test_demo_key_aurapay_2026_public',
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

export const mockPayments: PaymentIntent[] = [
  {
    id: 'pi_89123456',
    merchantId: mockMerchant.id,
    amountCents: 12500, // 125.00 EUR
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
    amountCents: 4999, // 49.99 EUR
    currency: 'EUR',
    status: 'PARTIALLY_REFUNDED',
    isTest: true,
    description: 'Licenza Modulo Addon Analytics',
    customerEmail: 'giulia.bianchi@studio.it',
    authorizationCode: 'AUTH_772134',
    bankTransactionId: 'tx_bank_441299',
    refundedAmountCents: 1000, // 10.00 EUR rimborsati
    createdAt: '2026-07-24T16:45:00Z',
    updatedAt: '2026-07-25T09:12:00Z',
  },
  {
    id: 'pi_99410211',
    merchantId: mockMerchant.id,
    amountCents: 8900, // 89.00 EUR
    currency: 'EUR',
    status: 'FAILED',
    isTest: true,
    description: 'Rinnovo Servizi Server Cloud',
    customerEmail: 'supporto@devstudio.io',
    failureReason: 'Fondi insufficienti sulla carta (Codice errore: 51)',
    createdAt: '2026-07-24T14:10:00Z',
    updatedAt: '2026-07-24T14:10:02Z',
  },
  {
    id: 'pi_00129944',
    merchantId: mockMerchant.id,
    amountCents: 35000, // 350.00 EUR
    currency: 'EUR',
    status: 'SUCCEEDED',
    isTest: false, // LIVE
    description: 'Consulenza Architetturale Microservizi',
    customerEmail: 'direzione@enterprisecorp.de',
    authorizationCode: 'AUTH_001299',
    bankTransactionId: 'tx_bank_776102',
    createdAt: '2026-07-23T18:30:00Z',
    updatedAt: '2026-07-23T18:30:04Z',
  },
  {
    id: 'pi_33219088',
    merchantId: mockMerchant.id,
    amountCents: 1999, // 19.99 EUR
    currency: 'EUR',
    status: 'REFUNDED',
    isTest: true,
    description: 'Estensione Spazio Storage 100GB',
    customerEmail: 'admin@startup.fr',
    authorizationCode: 'AUTH_445100',
    bankTransactionId: 'tx_bank_332190',
    refundedAmountCents: 1999,
    createdAt: '2026-07-22T10:15:00Z',
    updatedAt: '2026-07-22T11:00:00Z',
  },
];

export const mockLedgerBalances: Record<'TEST' | 'LIVE', LedgerBalance> = {
  TEST: {
    merchantId: mockMerchant.id,
    availableBalanceCents: 16499, // 164.99 EUR
    currency: 'EUR',
    isTest: true,
  },
  LIVE: {
    merchantId: mockMerchant.id,
    availableBalanceCents: 35000, // 350.00 EUR
    currency: 'EUR',
    isTest: false,
  },
};

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
  {
    id: 'led_02',
    merchantId: mockMerchant.id,
    paymentIntentId: 'pi_89123456',
    accountType: 'SYSTEM_REVENUE',
    entryType: 'CREDIT',
    amountCents: 375,
    isTest: true,
    createdAt: '2026-07-25T11:20:05Z',
  },
  {
    id: 'led_03',
    merchantId: mockMerchant.id,
    paymentIntentId: 'pi_89123456',
    accountType: 'SETTLEMENT_HOLDING',
    entryType: 'DEBIT',
    amountCents: 12500,
    isTest: true,
    createdAt: '2026-07-25T11:20:05Z',
  },
  {
    id: 'led_04',
    merchantId: mockMerchant.id,
    paymentIntentId: 'pi_00129944',
    accountType: 'MERCHANT_AVAILABLE',
    entryType: 'CREDIT',
    amountCents: 33950,
    isTest: false,
    createdAt: '2026-07-23T18:30:04Z',
  },
];

export const mockInvoices: Invoice[] = [
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
  {
    id: 'inv_2026_00045',
    invoiceNumber: 'INV-2026-000045',
    merchantId: mockMerchant.id,
    paymentIntentId: 'pi_00129944',
    amountCents: 35000,
    currency: 'EUR',
    invoiceType: 'INVOICE',
    status: 'GENERATED',
    isTest: false,
    pdfObjectKey: 'invoices/2026/mch_acme_tech/INV-2026-000045.pdf',
    createdAt: '2026-07-23T18:30:05Z',
  },
  {
    id: 'cn_2026_00005',
    invoiceNumber: 'CN-2026-000005',
    merchantId: mockMerchant.id,
    paymentIntentId: 'pi_33219088',
    refundId: 're_11223344',
    amountCents: 1999,
    currency: 'EUR',
    invoiceType: 'CREDIT_NOTE',
    status: 'GENERATED',
    isTest: true,
    pdfObjectKey: 'invoices/2026/mch_acme_tech/CN-2026-000005.pdf',
    createdAt: '2026-07-22T11:00:02Z',
  },
];

export const mockWebhookSubscriptions: WebhookSubscription[] = [
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

export const mockWebhookDeliveries: WebhookDelivery[] = [
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
  {
    id: 'del_02',
    subscriptionId: 'sub_01',
    eventId: 'evt_99f012aa',
    eventType: 'refund.succeeded',
    httpStatus: 500,
    attemptCount: 5,
    status: 'DEAD_LETTER',
    lastError: 'HTTP 500 Internal Server Error (Endpoint del merchant temporaneamente non raggiungibile)',
    nextAttemptAt: null,
    isTest: true,
    createdAt: '2026-07-24T16:46:00Z',
  },
];
