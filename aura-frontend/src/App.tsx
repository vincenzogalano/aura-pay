import React from 'react';
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom';
import { MerchantProvider } from './context/MerchantContext';
import { DashboardLayout } from './components/layout/DashboardLayout';
import { DashboardOverviewPage } from './pages/DashboardOverviewPage';
import { TransactionsPage } from './pages/TransactionsPage';
import { OnboardingPage } from './pages/OnboardingPage';
import { ApiKeysPage } from './pages/ApiKeysPage';
import { InvoicesPage } from './pages/InvoicesPage';
import { WebhooksPage } from './pages/WebhooksPage';
import { CheckoutSimulatorPage } from './pages/CheckoutSimulatorPage';
import { DeveloperConsolePage } from './pages/DeveloperConsolePage';
import { EventStreamPage } from './pages/EventStreamPage';
import { Toaster } from 'sonner';

export const App: React.FC = () => {
  return (
    <MerchantProvider>
      <Toaster position="top-right" theme="light" richColors />
      <HashRouter>
        <Routes>
          <Route path="/" element={<DashboardLayout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<DashboardOverviewPage />} />
            <Route path="transactions" element={<TransactionsPage />} />
            <Route path="onboarding" element={<OnboardingPage />} />
            <Route path="api-keys" element={<ApiKeysPage />} />
            <Route path="invoices" element={<InvoicesPage />} />
            <Route path="webhooks" element={<WebhooksPage />} />
            <Route path="checkout-demo" element={<CheckoutSimulatorPage />} />
            <Route path="developer" element={<DeveloperConsolePage />} />
            <Route path="event-stream" element={<EventStreamPage />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Route>
        </Routes>
      </HashRouter>
    </MerchantProvider>
  );
};

export default App;
