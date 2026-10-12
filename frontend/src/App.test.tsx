import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import App from "./App";

const apiTrips = [
  {
    id: "trip-001",
    origin: { name: "Santiago", latitude: -33.4489, longitude: -70.6693 },
    destination: { name: "Valparaiso", latitude: -33.0472, longitude: -71.6127 },
    driver: { id: "driver-001", name: "Carlos Soto" },
    vehicle: { id: "vehicle-001", plate: "FH-82-19" },
    status: "IN_TRANSIT",
    departureTime: "2026-10-11T12:00:00Z",
    estimatedArrival: "2026-10-11T14:00:00Z",
    progress: 58,
    totalDistanceKm: 120,
    travelledDistanceKm: 69.6,
    lastPosition: {
      vehicleId: "vehicle-001",
      tripId: "trip-001",
      latitude: -33.2321,
      longitude: -71.0201,
      speedKph: 82.5,
      fuelPercent: 44.2,
      odometerKm: 128540.7,
      recordedAt: "2026-10-11T13:15:30Z",
    },
    routeCoordinates: [
      [-33.4489, -70.6693],
      [-33.2321, -71.0201],
      [-33.0472, -71.6127],
    ],
    alerts: [
      {
        id: 100,
        tripId: "trip-001",
        vehicleId: "vehicle-001",
        type: "SPEEDING",
        severity: "WARNING",
        title: "Exceso de velocidad",
        description: "Velocidad reportada sobre el limite configurado",
        status: "OPEN",
        detectedAt: "2026-10-11T13:16:00Z",
      },
    ],
    timeline: [
      { label: "Viaje creado", state: "completed" },
      { label: "En ruta", state: "current" },
      { label: "Entrega", state: "pending" },
    ],
  },
  {
    id: "trip-002",
    origin: { name: "Santiago", latitude: -33.4489, longitude: -70.6693 },
    destination: { name: "Rancagua", latitude: -34.1708, longitude: -70.7444 },
    driver: { id: "driver-002", name: "Mariana Rojas" },
    vehicle: { id: "vehicle-002", plate: "LK-40-22" },
    status: "DELAYED",
    departureTime: "2026-10-11T12:10:00Z",
    estimatedArrival: "2026-10-11T15:10:00Z",
    progress: 42,
    totalDistanceKm: 87,
    travelledDistanceKm: 36.54,
    lastPosition: {
      vehicleId: "vehicle-002",
      tripId: "trip-002",
      latitude: -33.8976,
      longitude: -70.7358,
      speedKph: 65,
      fuelPercent: 61,
      odometerKm: 83610.4,
      recordedAt: "2026-10-11T13:20:00Z",
    },
    routeCoordinates: [
      [-33.4489, -70.6693],
      [-33.8976, -70.7358],
      [-34.1708, -70.7444],
    ],
    alerts: [],
    timeline: [
      { label: "Viaje creado", state: "completed" },
      { label: "Retraso detectado", state: "current" },
    ],
  },
  {
    id: "trip-003",
    origin: { name: "Santiago", latitude: -33.4489, longitude: -70.6693 },
    destination: { name: "San Antonio", latitude: -33.5922, longitude: -71.6217 },
    driver: { id: "driver-003", name: "Ignacio Perez" },
    vehicle: { id: "vehicle-003", plate: "PR-75-30" },
    status: "INCIDENT",
    departureTime: "2026-10-11T12:20:00Z",
    estimatedArrival: "2026-10-11T15:20:00Z",
    progress: 31,
    totalDistanceKm: 108,
    travelledDistanceKm: 33.48,
    lastPosition: null,
    routeCoordinates: [
      [-33.4489, -70.6693],
      [-33.5102, -71.243],
      [-33.5922, -71.6217],
    ],
    alerts: [
      {
        id: 301,
        tripId: "trip-003",
        vehicleId: "vehicle-003",
        type: "LOW_FUEL",
        severity: "CRITICAL",
        title: "Combustible bajo",
        description: "El vehiculo reporta combustible bajo el umbral configurado",
        status: "OPEN",
        detectedAt: "2026-10-11T13:25:00Z",
      },
    ],
    timeline: [{ label: "Incidente registrado", state: "current" }],
  },
  {
    id: "trip-004",
    origin: { name: "Concepcion", latitude: -36.8201, longitude: -73.0444 },
    destination: { name: "Los Angeles", latitude: -37.4697, longitude: -72.3537 },
    driver: { id: "driver-004", name: "Valentina Munoz" },
    vehicle: { id: "vehicle-004", plate: "ZX-11-48" },
    status: "SCHEDULED",
    departureTime: "2026-10-11T16:00:00Z",
    estimatedArrival: "2026-10-11T19:00:00Z",
    progress: 0,
    totalDistanceKm: 130,
    travelledDistanceKm: 0,
    lastPosition: null,
    routeCoordinates: [
      [-36.8201, -73.0444],
      [-37.4697, -72.3537],
    ],
    alerts: [],
    timeline: [{ label: "Viaje creado", state: "current" }],
  },
];

async function renderDashboard() {
  render(<App />);
  await screen.findByRole("heading", { name: "Viajes activos" });
  await screen.findByRole("button", { name: /trip-001/i });
}

describe("FleetOps dispatch dashboard", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn((input: RequestInfo | URL) => {
        const url = String(input);
        const path = url.replace(/^https?:\/\/[^/]+/, "");

        if (path === "/api/v1/trips") {
          return Promise.resolve(Response.json(apiTrips));
        }

        const tripMatch = path.match(/^\/api\/v1\/trips\/([^/]+)$/);
        if (tripMatch) {
          const trip = apiTrips.find((item) => item.id === tripMatch[1]);
          return Promise.resolve(trip ? Response.json(trip) : new Response(null, { status: 404 }));
        }

        return Promise.resolve(new Response(null, { status: 404 }));
      })
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renderiza el dashboard operativo", async () => {
    await renderDashboard();

    expect(screen.getAllByText("FleetOps").length).toBeGreaterThan(0);
    expect(screen.getByRole("main", { name: "Panel de despacho" })).toBeInTheDocument();
  });

  it("muestra la lista de viajes", async () => {
    await renderDashboard();

    expect(await screen.findByRole("button", { name: /trip-001/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /trip-002/i })).toBeInTheDocument();
  });

  it("cambia el viaje seleccionado", async () => {
    const user = userEvent.setup();
    await renderDashboard();

    await user.click(screen.getByRole("button", { name: /trip-002/i }));

    expect(screen.getByRole("heading", { name: "trip-002" })).toBeInTheDocument();
    expect(screen.getAllByText(/Santiago.*Rancagua/).length).toBeGreaterThan(0);
  });

  it("actualiza la informacion del viaje seleccionado", async () => {
    const user = userEvent.setup();
    await renderDashboard();

    await user.click(screen.getByRole("button", { name: /trip-003/i }));

    expect(screen.getAllByText("Ignacio Perez").length).toBeGreaterThan(0);
    expect(screen.getAllByText("PR-75-30").length).toBeGreaterThan(0);
    expect(screen.getByText("31%")).toBeInTheDocument();
  });

  it("filtra por estado", async () => {
    const user = userEvent.setup();
    await renderDashboard();

    await user.click(screen.getByRole("button", { name: "Con retraso" }));

    expect(screen.getByRole("button", { name: /trip-002/i })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /trip-001/i })).not.toBeInTheDocument();
  });

  it("renderiza el panel de alertas", async () => {
    await renderDashboard();

    expect(screen.getByRole("heading", { name: "Alertas e incidentes" })).toBeInTheDocument();
    expect(screen.getByText("Exceso de velocidad")).toBeInTheDocument();
  });

  it("muestra estado vacio de alertas", async () => {
    const user = userEvent.setup();
    await renderDashboard();

    await user.click(screen.getByRole("button", { name: /trip-004/i }));

    expect(screen.getByText("Sin alertas activas")).toBeInTheDocument();
  });

  it("incluye el contenedor del mapa", async () => {
    await renderDashboard();

    const map = screen.getByTestId("fleet-map");
    expect(map).toBeInTheDocument();
    expect(within(map).getByTestId("tile-layer")).toBeInTheDocument();
    expect(within(map).getByTestId("trip-polyline")).toBeInTheDocument();
  });
});
