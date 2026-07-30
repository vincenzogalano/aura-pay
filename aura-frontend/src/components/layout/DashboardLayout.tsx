import React from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { A11yInspector } from '../common/A11yInspector';

export const DashboardLayout: React.FC = () => {
  return (
    <div className="flex min-h-screen bg-white text-zinc-900">
      {/* Sidebar Navigation */}
      <Sidebar />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        <Header />
        
        {/* Page Container */}
        <main className="flex-1 p-6 md:p-8 overflow-y-auto bg-white" id="main-content">
          <div className="max-w-7xl mx-auto space-y-8">
            <Outlet />
          </div>
        </main>
      </div>

      {/* WCAG Accessibility & Contrast Inspector Widget */}
      <A11yInspector />
    </div>
  );
};
