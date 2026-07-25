import React from 'react';
import { ShieldAlert, ShieldCheck } from 'lucide-react';

interface EnvironmentBadgeProps {
  isTest: boolean;
}

export const EnvironmentBadge: React.FC<EnvironmentBadgeProps> = ({ isTest }) => {
  if (isTest) {
    return (
      <div className="badge-sandbox transition-all duration-300">
        <span className="relative flex h-2 w-2">
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
          <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-500"></span>
        </span>
        <ShieldAlert className="w-3.5 h-3.5" />
        <span>SANDBOX (TEST)</span>
      </div>
    );
  }

  return (
    <div className="badge-live transition-all duration-300">
      <span className="relative flex h-2 w-2">
        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
        <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
      </span>
      <ShieldCheck className="w-3.5 h-3.5" />
      <span>LIVE MODE</span>
    </div>
  );
};
