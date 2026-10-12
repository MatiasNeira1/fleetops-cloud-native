import { RadioTower, RefreshCw, TriangleAlert } from "lucide-react";
import type { Trip } from "../types/fleet";
import { StatusBadge } from "./StatusBadge";
import { TripMap } from "./TripMap";
import { TripProgress } from "./TripProgress";
import { TripTimeline } from "./TripTimeline";

interface TripDetailsProps {
  trip: Trip;
  onReportIncident: (trip: Trip) => void;
  onSendTelemetry: (trip: Trip) => void;
  onRefresh: () => void;
}

export function TripDetails({
  trip,
  onReportIncident,
  onSendTelemetry,
  onRefresh,
}: TripDetailsProps) {
  return (
    <section className="panel detail-panel" aria-labelledby="trip-detail-title">
      <div className="detail-header">
        <div>
          <p className="eyebrow">Viaje seleccionado</p>
          <div className="detail-title-row">
            <h2 id="trip-detail-title">{trip.id}</h2>
            <StatusBadge label={trip.status} />
          </div>
          <p className="detail-route">
            {trip.origin.name} → {trip.destination.name}
          </p>
        </div>
        <div className="action-row">
          <button type="button" className="secondary-button" onClick={() => onRefresh()}>
            <RefreshCw aria-hidden="true" />
            Actualizar
          </button>
          <button type="button" className="secondary-button" onClick={() => onSendTelemetry(trip)}>
            <RadioTower aria-hidden="true" />
            Enviar telemetria
          </button>
          <button type="button" className="primary-button" onClick={() => onReportIncident(trip)}>
            <TriangleAlert aria-hidden="true" />
            Reportar incidente
          </button>
        </div>
      </div>

      <dl className="trip-summary">
        <div>
          <dt>Salida</dt>
          <dd>{trip.departureTime}</dd>
        </div>
        <div>
          <dt>ETA</dt>
          <dd>{trip.estimatedArrival}</dd>
        </div>
        <div>
          <dt>Conductor</dt>
          <dd>{trip.driver}</dd>
        </div>
        <div>
          <dt>Vehiculo</dt>
          <dd>{trip.vehicle}</dd>
        </div>
      </dl>

      <TripMap trip={trip} />
      <TripProgress trip={trip} />
      <TripTimeline timeline={trip.timeline} />
    </section>
  );
}
