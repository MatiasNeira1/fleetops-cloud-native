import { useEffect, useMemo } from "react";
import L from "leaflet";
import { MapContainer, Marker, Polyline, Popup, TileLayer, Tooltip, useMap } from "react-leaflet";
import type { Coordinates, Trip, TripStatus } from "../types/fleet";

const statusRouteColors: Record<TripStatus, string> = {
  "En ruta": "#14b8a6",
  "Con retraso": "#f97316",
  Incidente: "#ef4444",
  Programado: "#64748b",
};

const chileCenter: Coordinates = [-33.45, -70.66];

function createMarkerIcon(kind: "origin" | "vehicle" | "destination" | "incident") {
  return L.divIcon({
    className: `fleet-marker fleet-marker-${kind}`,
    html: `<span aria-hidden="true"></span>`,
    iconSize: [28, 28],
    iconAnchor: [14, 14],
    popupAnchor: [0, -14],
  });
}

const markerIcons = {
  origin: createMarkerIcon("origin"),
  vehicle: createMarkerIcon("vehicle"),
  destination: createMarkerIcon("destination"),
  incident: createMarkerIcon("incident"),
};

interface MapViewportControllerProps {
  points: Coordinates[];
}

export function MapViewportController({ points }: MapViewportControllerProps) {
  const map = useMap();

  useEffect(() => {
    const bounds = L.latLngBounds(points);
    map.fitBounds(bounds, { padding: [34, 34], maxZoom: 11 });

    const resizeTimer = window.setTimeout(() => {
      map.invalidateSize();
    }, 180);

    return () => window.clearTimeout(resizeTimer);
  }, [map, points]);

  return null;
}

interface TripMapProps {
  trip: Trip;
}

export function TripMap({ trip }: TripMapProps) {
  const routeColor = statusRouteColors[trip.status];
  const viewportPoints = useMemo(
    () => [
      trip.origin.coordinates,
      ...trip.routeCoordinates,
      trip.currentPosition,
      trip.destination.coordinates,
    ],
    [trip]
  );
  const vehicleIcon = trip.status === "Incidente" ? markerIcons.incident : markerIcons.vehicle;

  return (
    <section className="map-section" aria-label={`Mapa del viaje ${trip.id}`}>
      <MapContainer
        center={chileCenter}
        zoom={7}
        className="fleet-map"
        zoomControl
        scrollWheelZoom
      >
        <MapViewportController points={viewportPoints} />
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <Polyline
          positions={trip.routeCoordinates}
          pathOptions={{
            color: routeColor,
            weight: 6,
            opacity: 0.86,
            lineCap: "round",
          }}
        />
        <Marker position={trip.origin.coordinates} icon={markerIcons.origin}>
          <Popup>
            <strong>Origen</strong>
            <br />
            {trip.origin.name}
          </Popup>
          <Tooltip>Origen</Tooltip>
        </Marker>
        <Marker position={trip.currentPosition} icon={vehicleIcon}>
          <Popup>
            <strong>Vehiculo {trip.vehicle}</strong>
            <br />
            {trip.driver}
            <br />
            Estado: {trip.status}
          </Popup>
          <Tooltip permanent direction="top">
            {trip.vehicle}
          </Tooltip>
        </Marker>
        <Marker position={trip.destination.coordinates} icon={markerIcons.destination}>
          <Popup>
            <strong>Destino</strong>
            <br />
            {trip.destination.name}
          </Popup>
          <Tooltip>Destino</Tooltip>
        </Marker>
      </MapContainer>
    </section>
  );
}
