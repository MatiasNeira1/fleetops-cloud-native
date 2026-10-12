import type {
  AlertSeverity,
  Coordinates,
  TimelineState,
  Trip,
  TripAlert,
  TripStatus,
} from "../types/fleet";

type ApiTripStatus = "IN_TRANSIT" | "DELAYED" | "INCIDENT" | "SCHEDULED";
type ApiAlertSeverity = "CRITICAL" | "WARNING" | "INFO" | "INFORMATIONAL";

interface ApiPlace {
  name: string;
  latitude: number | string;
  longitude: number | string;
}

interface ApiPerson {
  id: string;
  name?: string;
  plate?: string;
}

interface ApiPosition {
  latitude: number | string;
  longitude: number | string;
}

interface ApiTimelineItem {
  label: string;
  state: string;
}

interface ApiAlert {
  id: number;
  tripId: string;
  vehicleId: string;
  severity: ApiAlertSeverity;
  title: string;
  description: string;
  status: "OPEN" | "RESOLVED";
  detectedAt: string;
}

interface ApiTripSummary {
  id: string;
  origin: ApiPlace;
  destination: ApiPlace;
  driver: ApiPerson;
  vehicle: ApiPerson;
  status: ApiTripStatus;
  departureTime: string;
  estimatedArrival: string;
  progress: number;
  totalDistanceKm: number | string;
  travelledDistanceKm: number | string;
}

interface ApiTripDetail extends ApiTripSummary {
  lastPosition: ApiPosition | null;
  routeCoordinates: Array<[number | string, number | string]>;
  alerts: ApiAlert[];
  timeline: ApiTimelineItem[];
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "";

const statusLabels: Record<ApiTripStatus, TripStatus> = {
  IN_TRANSIT: "En ruta",
  DELAYED: "Con retraso",
  INCIDENT: "Incidente",
  SCHEDULED: "Programado",
};

const severityLabels: Record<ApiAlertSeverity, AlertSeverity> = {
  CRITICAL: "Critico",
  WARNING: "Advertencia",
  INFO: "Informativo",
  INFORMATIONAL: "Informativo",
};

async function requestJson<T>(path: string): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`);
  if (!response.ok) {
    throw new Error(`FleetOps API error ${response.status} for ${path}`);
  }
  return response.json() as Promise<T>;
}

function toCoordinates(latitude: number | string, longitude: number | string): Coordinates {
  return [Number(latitude), Number(longitude)];
}

function formatTime(value: string): string {
  return new Intl.DateTimeFormat("es-CL", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(value));
}

function formatElapsedTime(departureTime: string): string {
  const elapsedMs = Math.max(0, Date.now() - new Date(departureTime).getTime());
  const totalMinutes = Math.floor(elapsedMs / 60000);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  if (hours <= 0) {
    return `${minutes} min`;
  }
  return `${hours} h ${minutes} min`;
}

function toTimelineState(state: string): TimelineState {
  if (state === "completed" || state === "current" || state === "pending") {
    return state;
  }
  return "pending";
}

function toTripAlert(alert: ApiAlert): TripAlert {
  return {
    id: String(alert.id),
    severity: severityLabels[alert.severity] ?? "Informativo",
    title: alert.title,
    description: alert.description,
    time: formatTime(alert.detectedAt),
    tripId: alert.tripId,
  };
}

function toTrip(detail: ApiTripDetail): Trip {
  const origin = toCoordinates(detail.origin.latitude, detail.origin.longitude);
  const destination = toCoordinates(detail.destination.latitude, detail.destination.longitude);
  const routeCoordinates = detail.routeCoordinates.length > 0
    ? detail.routeCoordinates.map(([latitude, longitude]) => toCoordinates(latitude, longitude))
    : [origin, destination];
  const currentPosition = detail.lastPosition
    ? toCoordinates(detail.lastPosition.latitude, detail.lastPosition.longitude)
    : routeCoordinates[Math.max(0, Math.floor(routeCoordinates.length / 2))] ?? origin;

  return {
    id: detail.id,
    origin: { name: detail.origin.name, coordinates: origin },
    destination: { name: detail.destination.name, coordinates: destination },
    driver: detail.driver.name ?? detail.driver.id,
    vehicleId: detail.vehicle.id,
    vehicle: detail.vehicle.plate ?? detail.vehicle.id,
    status: statusLabels[detail.status] ?? "Programado",
    departureTime: formatTime(detail.departureTime),
    estimatedArrival: formatTime(detail.estimatedArrival),
    progress: detail.progress,
    distance: {
      completedKm: Number(detail.travelledDistanceKm),
      totalKm: Number(detail.totalDistanceKm),
    },
    elapsedTime: formatElapsedTime(detail.departureTime),
    currentPosition,
    routeCoordinates,
    timeline: detail.timeline.map((item) => ({
      label: item.label,
      time: "",
      state: toTimelineState(item.state),
    })),
    alerts: detail.alerts.filter((alert) => alert.status === "OPEN").map(toTripAlert),
  };
}

export async function getActiveTrips(): Promise<Trip[]> {
  const summaries = await requestJson<ApiTripSummary[]>("/api/v1/trips");
  const details = await Promise.all(
    summaries.map((trip) => requestJson<ApiTripDetail>(`/api/v1/trips/${trip.id}`))
  );
  return details.map(toTrip);
}

export async function getTripById(id: string): Promise<Trip | undefined> {
  try {
    return toTrip(await requestJson<ApiTripDetail>(`/api/v1/trips/${id}`));
  } catch {
    return undefined;
  }
}

export async function getAlertsByTripId(tripId: string): Promise<TripAlert[]> {
  const alerts = await requestJson<ApiAlert[]>(`/api/v1/trips/${tripId}/alerts`);
  return alerts.filter((alert) => alert.status === "OPEN").map(toTripAlert);
}

async function sendJson<T>(path: string, method: "POST" | "PATCH", body: unknown): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    throw new Error(`FleetOps API error ${response.status} for ${path}`);
  }
  return response.json() as Promise<T>;
}

export async function resolveAlert(alertId: string): Promise<void> {
  await sendJson(`/api/v1/alerts/${alertId}/status`, "PATCH", { status: "RESOLVED" });
}

export async function createIncidentForTrip(trip: Trip): Promise<void> {
  await sendJson("/api/v1/incidents", "POST", {
    tripId: trip.id,
    vehicleId: trip.vehicleId,
    type: "GENERAL",
    title: "Incidente operativo",
    description: `Incidente registrado desde el panel para ${trip.id}`,
    severity: "WARNING",
    latitude: trip.currentPosition[0],
    longitude: trip.currentPosition[1],
  });
}

export async function sendTelemetryForTrip(trip: Trip): Promise<void> {
  await sendJson("/api/v1/telemetry", "POST", {
    eventId: crypto.randomUUID(),
    vehicleId: trip.vehicleId,
    tripId: trip.id,
    latitude: trip.currentPosition[0],
    longitude: trip.currentPosition[1],
    speedKph: 96.5,
    fuelPercent: 10.5,
    odometerKm: Math.round((trip.distance.completedKm + 100000) * 10) / 10,
    recordedAt: new Date().toISOString(),
  });
}
