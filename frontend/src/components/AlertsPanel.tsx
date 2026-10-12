import { AlertTriangle, CheckCircle2, Inbox } from "lucide-react";
import type { Trip } from "../types/fleet";
import { AlertCard } from "./AlertCard";

interface AlertsPanelProps {
  trip: Trip | null;
  onResolveAlert: (alertTitle: string) => void;
}

export function AlertsPanel({ trip, onResolveAlert }: AlertsPanelProps) {
  const alerts = trip?.alerts ?? [];

  return (
    <aside className="panel alerts-panel" aria-labelledby="alerts-title">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Monitoreo</p>
          <h2 id="alerts-title">Alertas e incidentes</h2>
        </div>
        <span className="panel-count">{alerts.length}</span>
      </div>

      {trip && (
        <div className="alert-context">
          <AlertTriangle aria-hidden="true" />
          <span>{trip.id}</span>
        </div>
      )}

      <div className="alerts-list">
        {alerts.length > 0 ? (
          alerts.map((alert) => (
            <AlertCard key={alert.id} alert={alert} onResolve={() => onResolveAlert(alert.title)} />
          ))
        ) : (
          <div className="empty-alerts">
            <Inbox aria-hidden="true" />
            <strong>Sin alertas activas</strong>
            <span>El viaje seleccionado no registra incidentes abiertos.</span>
            <CheckCircle2 aria-hidden="true" />
          </div>
        )}
      </div>
    </aside>
  );
}
