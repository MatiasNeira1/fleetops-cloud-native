import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import App from "./App";

async function renderDashboard() {
  render(<App />);
  await screen.findByRole("heading", { name: "Viajes activos" });
  await screen.findByRole("button", { name: /TRP-2048/i });
}

describe("FleetOps dispatch dashboard", () => {
  it("renderiza el dashboard operativo", async () => {
    await renderDashboard();

    expect(screen.getAllByText("FleetOps").length).toBeGreaterThan(0);
    expect(screen.getByRole("main", { name: "Panel de despacho" })).toBeInTheDocument();
  });

  it("muestra la lista de viajes", async () => {
    await renderDashboard();

    expect(await screen.findByRole("button", { name: /TRP-2048/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /TRP-2051/i })).toBeInTheDocument();
  });

  it("cambia el viaje seleccionado", async () => {
    const user = userEvent.setup();
    await renderDashboard();

    await user.click(screen.getByRole("button", { name: /TRP-2051/i }));

    expect(screen.getByRole("heading", { name: "TRP-2051" })).toBeInTheDocument();
    expect(screen.getAllByText("Santiago Norte → Rancagua Centro").length).toBeGreaterThan(0);
  });

  it("actualiza la informacion del viaje seleccionado", async () => {
    const user = userEvent.setup();
    await renderDashboard();

    await user.click(screen.getByRole("button", { name: /TRP-2057/i }));

    expect(screen.getAllByText("Valentina Herrera").length).toBeGreaterThan(0);
    expect(screen.getAllByText("KP-90-44").length).toBeGreaterThan(0);
    expect(screen.getByText("54%")).toBeInTheDocument();
  });

  it("filtra por estado", async () => {
    const user = userEvent.setup();
    await renderDashboard();

    await user.click(screen.getByRole("button", { name: "Con retraso" }));

    expect(screen.getByRole("button", { name: /TRP-2051/i })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /TRP-2048/i })).not.toBeInTheDocument();
  });

  it("renderiza el panel de alertas", async () => {
    await renderDashboard();

    expect(screen.getByRole("heading", { name: "Alertas e incidentes" })).toBeInTheDocument();
    expect(screen.getByText("Entrega proxima")).toBeInTheDocument();
  });

  it("muestra estado vacio de alertas", async () => {
    const user = userEvent.setup();
    await renderDashboard();

    await user.click(screen.getByRole("button", { name: /TRP-2062/i }));

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
