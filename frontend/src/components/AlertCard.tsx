import { ArrowRight, ShieldCheck } from "lucide-react";
import type { TripAlert } from "../types/fleet";
import { StatusBadge } from "./StatusBadge";

interface AlertCardProps {
  alert: TripAlert;
  onResolve: () => void;
}

export function AlertCard({ alert, onResolve }: AlertCardProps) {
  return (
    <article className={`alert-card severity-${alert.severity.toLowerCase()}`}>
      <div className="alert-card-top">
        <StatusBadge label={alert.severity} tone="alert" />
        <span>{alert.time}</span>
      </div>
      <h3>{alert.title}</h3>
      <p>{alert.description}</p>
      <div className="alert-card-footer">
        <span>{alert.tripId}</span>
        <button type="button" onClick={onResolve}>
          <ShieldCheck aria-hidden="true" />
          Resolver alerta
          <ArrowRight aria-hidden="true" />
        </button>
      </div>
    </article>
  );
}
