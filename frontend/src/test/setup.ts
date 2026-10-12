import "@testing-library/jest-dom/vitest";
import type { ReactNode } from "react";
import { vi } from "vitest";

vi.mock("react-leaflet", async () => {
  const React = await import("react");

  return {
    MapContainer: ({ children, className }: { children: ReactNode; className?: string }) =>
      React.createElement("div", { className, "data-testid": "fleet-map" }, children),
    TileLayer: () => React.createElement("div", { "data-testid": "tile-layer" }),
    Polyline: () => React.createElement("div", { "data-testid": "trip-polyline" }),
    Marker: ({ children }: { children: ReactNode }) =>
      React.createElement("div", { "data-testid": "trip-marker" }, children),
    Popup: ({ children }: { children: ReactNode }) =>
      React.createElement("div", null, children),
    Tooltip: ({ children }: { children: ReactNode }) =>
      React.createElement("div", null, children),
    useMap: () => ({
      fitBounds: vi.fn(),
      invalidateSize: vi.fn(),
    }),
  };
});
