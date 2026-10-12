import { Clock, MapPin, Truck } from "lucide-react";
import type { Trip } from "../types/fleet";
import { StatusBadge } from "./StatusBadge";

interface TripListItemProps {
  trip: Trip;
  isSelected: boolean;
  onSelect: () => void;
}

export function TripListItem({ trip, isSelected, onSelect }: TripListItemProps) {
  return (
    <button
      className={isSelected ? "trip-card selected" : "trip-card"}
      type="button"
      onClick={onSelect}
      aria-pressed={isSelected}
    >
      <span className="trip-card-header">
        <strong>{trip.id}</strong>
        <StatusBadge label={trip.status} />
      </span>
      <span className="route-line">
        <MapPin aria-hidden="true" />
        {trip.origin.name} → {trip.destination.name}
      </span>
      <span className="trip-card-meta">
        <span>
          <Truck aria-hidden="true" />
          {trip.vehicle}
        </span>
        <span>
          <Clock aria-hidden="true" />
          ETA {trip.estimatedArrival}
        </span>
      </span>
      <span className="driver-line">{trip.driver}</span>
    </button>
  );
}
