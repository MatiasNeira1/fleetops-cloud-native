export type Coordinates = [number, number];

export type TripStatus =
  | "En ruta"
  | "Con retraso"
  | "Incidente"
  | "Programado";

export type TripStatusFilter = "Todos" | "En ruta" | "Con retraso" | "Incidente";

export type TimelineState = "completed" | "current" | "pending";

export type AlertSeverity =
  | "Critico"
  | "Advertencia"
  | "Informativo"
  | "Resuelto";

export interface Place {
  name: string;
  coordinates: Coordinates;
}

export interface TripTimelineItem {
  label: string;
  time: string;
  state: TimelineState;
}

export interface TripAlert {
  id: string;
  severity: AlertSeverity;
  title: string;
  description: string;
  time: string;
  tripId: string;
}

export interface Trip {
  id: string;
  origin: Place;
  destination: Place;
  driver: string;
  vehicleId: string;
  vehicle: string;
  status: TripStatus;
  departureTime: string;
  estimatedArrival: string;
  progress: number;
  distance: {
    completedKm: number;
    totalKm: number;
  };
  elapsedTime: string;
  currentPosition: Coordinates;
  routeCoordinates: Coordinates[];
  timeline: TripTimelineItem[];
  alerts: TripAlert[];
}
