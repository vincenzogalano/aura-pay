import React from 'react';

interface EnvironmentBadgeProps {
  isTest: boolean;
}

export const EnvironmentBadge: React.FC<EnvironmentBadgeProps> = ({ isTest }) => {
  if (isTest) {
    return (
      <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded border bg-amber-950/60 text-amber-400 border-amber-800 text-[11px] font-mono font-medium">
        <span className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-pulse" />
        <span>SANDBOX</span>
      </div>
    );
  }

  return (
    <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded border bg-emerald-950/60 text-emerald-400 border-emerald-800 text-[11px] font-mono font-medium">
      <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
      <span>LIVE</span>
    </div>
  );
};
