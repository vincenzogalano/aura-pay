export type KYBStatus = 'PENDING_VERIFICATION' | 'VERIFIED' | 'VERIFICATION_REJECTED' | 'SUSPENDED';

export interface Merchant {
  id: string;
  businessName: string;
  vatNumber: string;
  email: string;
  status: KYBStatus;
  country: string;
  createdAt: string;
}

export interface ApiKey {
  id: string;
  merchantId: string;
  keyPrefix: string;
  environment: 'TEST' | 'LIVE';
  keyType?: string;
  rawKey?: string;
  revokedAt: string | null;
  createdAt: string;
  keySecret?: string; // Esibito solo al momento della creazione
}

export type PaymentStatus = 
  | 'CREATED' 
  | 'PROCESSING' 
  | 'SUCCEEDED' 
  | 'FAILED' 
  | 'CANCELLED' 
  | 'REFUNDED' 
  | 'PARTIALLY_REFUNDED';

export interface PaymentIntent {
  id: string;
  merchantId: string;
  amountCents: number;
  currency: string;
  status: PaymentStatus;
  isTest: boolean;
  description?: string;
  customerEmail?: string;
  authorizationCode?: string;
  bankTransactionId?: string;
  failureReason?: string;
  refundedAmountCents?: number;
  createdAt: string;
  updatedAt: string;
}

export interface LedgerEntry {
  id: string;
  merchantId: string;
  paymentIntentId?: string;
  refundId?: string;
  accountType: 'MERCHANT_AVAILABLE' | 'SYSTEM_REVENUE' | 'SETTLEMENT_HOLDING';
  entryType: 'DEBIT' | 'CREDIT';
  amountCents: number;
  isTest: boolean;
  createdAt: string;
}

export interface LedgerBalance {
  merchantId: string;
  availableBalanceCents: number;
  currency: string;
  isTest: boolean;
}

export type InvoiceType = 'INVOICE' | 'CREDIT_NOTE';
export type InvoiceStatus = 'GENERATED' | 'FAILED';

export interface Invoice {
  id: string;
  invoiceNumber: string;
  merchantId: string;
  paymentIntentId?: string;
  refundId?: string;
  amountCents: number;
  currency: string;
  invoiceType: InvoiceType;
  status: InvoiceStatus;
  isTest: boolean;
  pdfObjectKey: string;
  createdAt: string;
}

export interface WebhookSubscription {
  id: string;
  merchantId: string;
  targetUrl: string;
  events: string[];
  secretKey: string;
  isTest: boolean;
  createdAt: string;
}

export type DeliveryStatus = 'SUCCESS' | 'PENDING' | 'DEAD_LETTER';

export interface WebhookDelivery {
  id: string;
  subscriptionId: string;
  eventId: string;
  eventType: string;
  httpStatus: number | null;
  attemptCount: number;
  status: DeliveryStatus;
  lastError: string | null;
  nextAttemptAt: string | null;
  isTest: boolean;
  createdAt: string;
}
