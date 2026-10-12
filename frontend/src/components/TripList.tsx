import { AlertCircle, Loader2, SearchX } from "lucide-react";
import type { Trip } from "../types/fleet";
import { TripListItem } from "./TripListItem";

interface TripListProps {
  trips: Trip[];
  selectedTripId: string | null;
  isLoading: boolean;
  error: string | null;
  onSelectTrip: (tripId: string) => void;
}

export function TripList({
  trips,
  selectedTripId,
  isLoading,
  error,
  onSelectTrip,
}: TripListProps) {
  if (isLoading) {
    return (
      <div className="list-state" role="status">
        <Loader2 className="spin" aria-hidden="true" />
        Cargando viajes activos...
      </div>
    );
  }

  if (error) {
    return (
      <div className="list-state error" role="alert">
        <AlertCircle aria-hidden="true" />
        {error}
      </div>
    );
  }

  if (trips.length === 0) {
    return (
      <div className="list-state">
        <SearchX aria-hidden="true" />
        <strong>Sin coincidencias</strong>
        <span>No hay viajes para el filtro seleccionado.</span>
      </div>
    );
  }

  return (
    <div className="trip-list" aria-label="Lista de viajes activos">
      {trips.map((trip) => (
        <TripListItem
          key={trip.id}
          trip={trip}
          isSelected={trip.id === selectedTripId}
          onSelect={() => onSelectTrip(trip.id)}
        />
      ))}
    </div>
  );
}
