import type { Trip } from "../types/fleet";

interface TripProgressProps {
  trip: Trip;
}

export function TripProgress({ trip }: TripProgressProps) {
  return (
    <section className="progress-card" aria-label="Progreso del viaje">
      <div className="progress-stats">
        <div>
          <span>Distancia recorrida</span>
          <strong>{trip.distance.completedKm} km</strong>
        </div>
        <div>
          <span>Distancia total</span>
          <strong>{trip.distance.totalKm} km</strong>
        </div>
        <div>
          <span>Tiempo transcurrido</span>
          <strong>{trip.elapsedTime}</strong>
        </div>
        <div>
          <span>Completado</span>
          <strong>{trip.progress}%</strong>
        </div>
      </div>
      <div className="progress-track" aria-label={`${trip.progress}% completado`}>
        <span style={{ width: `${trip.progress}%` }} />
      </div>
    </section>
  );
}
