import { MessageSquare, Route, Truck } from "lucide-react";
import type { Trip } from "../types/fleet";
import { StatusBadge } from "./StatusBadge";
import { TripMap } from "./TripMap";
import { TripProgress } from "./TripProgress";
import { TripTimeline } from "./TripTimeline";

interface TripDetailsProps {
  trip: Trip;
  onAction: (message: string) => void;
}

export function TripDetails({ trip, onAction }: TripDetailsProps) {
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
          <button type="button" className="secondary-button" onClick={() => onAction(`Vehiculo asignado a ${trip.id}`)}>
            <Truck aria-hidden="true" />
            Asignar vehiculo
          </button>
          <button type="button" className="secondary-button" onClick={() => onAction(`Contacto abierto con ${trip.driver}`)}>
            <MessageSquare aria-hidden="true" />
            Contactar conductor
          </button>
          <button type="button" className="primary-button" onClick={() => onAction(`Detalle operativo de ${trip.id}`)}>
            <Route aria-hidden="true" />
            Ver detalle
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
