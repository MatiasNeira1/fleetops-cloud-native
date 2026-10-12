import type { ReactNode } from "react";

interface FleetOpsLayoutProps {
  sidebar: ReactNode;
  topbar: ReactNode;
  leftPanel: ReactNode;
  mainPanel: ReactNode;
  rightPanel: ReactNode;
  children?: ReactNode;
}

export function FleetOpsLayout({
  sidebar,
  topbar,
  leftPanel,
  mainPanel,
  rightPanel,
  children,
}: FleetOpsLayoutProps) {
  return (
    <div className="app-shell">
      {sidebar}
      <div className="workspace">
        {topbar}
        <main className="dashboard-grid" aria-label="Panel de despacho">
          {leftPanel}
          {mainPanel}
          {rightPanel}
        </main>
      </div>
      {children}
    </div>
  );
}
