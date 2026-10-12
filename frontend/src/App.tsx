import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, Search, X } from "lucide-react";
import { FleetOpsLayout } from "./components/FleetOpsLayout";
import { TopBar } from "./components/TopBar";
import { Sidebar } from "./components/Sidebar";
import { TripFilters } from "./components/TripFilters";
import { TripList } from "./components/TripList";
import { TripDetails } from "./components/TripDetails";
import { AlertsPanel } from "./components/AlertsPanel";
import { getActiveTrips } from "./services/tripService";
import type { Trip, TripStatusFilter } from "./types/fleet";

function App() {
  const [trips, setTrips] = useState<Trip[]>([]);
  const [selectedTripId, setSelectedTripId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<TripStatusFilter>("Todos");
  const [query, setQuery] = useState("");
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    getActiveTrips()
      .then((activeTrips) => {
        if (!isMounted) {
          return;
        }
        setTrips(activeTrips);
        setSelectedTripId(activeTrips[0]?.id ?? null);
        setIsLoading(false);
      })
      .catch(() => {
        if (!isMounted) {
          return;
        }
        setError("No fue posible cargar los viajes activos.");
        setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const filteredTrips = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return trips.filter((trip) => {
      const matchesStatus =
        statusFilter === "Todos" || trip.status === statusFilter;
      const matchesQuery =
        normalizedQuery.length === 0 ||
        [
          trip.id,
          trip.origin.name,
          trip.destination.name,
          trip.driver,
          trip.vehicle,
        ]
          .join(" ")
          .toLowerCase()
          .includes(normalizedQuery);

      return matchesStatus && matchesQuery;
    });
  }, [query, statusFilter, trips]);

  const selectedTrip =
    (selectedTripId
      ? trips.find((trip) => trip.id === selectedTripId)
      : undefined) ?? filteredTrips[0] ?? null;

  const showToast = (message: string) => {
    setToast(message);
    window.setTimeout(() => setToast(null), 2600);
  };

  return (
    <FleetOpsLayout
      sidebar={
        <Sidebar
          isOpen={isSidebarOpen}
          onClose={() => setIsSidebarOpen(false)}
        />
      }
      topbar={
        <TopBar
          query={query}
          onQueryChange={setQuery}
          onMenuClick={() => setIsSidebarOpen((current) => !current)}
        />
      }
      leftPanel={
        <section className="panel trip-panel" aria-labelledby="active-trips">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">Despacho</p>
              <h1 id="active-trips">Viajes activos</h1>
            </div>
            <span className="panel-count">{filteredTrips.length}</span>
          </div>
          <TripFilters
            activeFilter={statusFilter}
            onFilterChange={setStatusFilter}
          />
          <TripList
            trips={filteredTrips}
            selectedTripId={selectedTrip?.id ?? null}
            isLoading={isLoading}
            error={error}
            onSelectTrip={setSelectedTripId}
          />
        </section>
      }
      mainPanel={
        selectedTrip ? (
          <TripDetails trip={selectedTrip} onAction={showToast} />
        ) : (
          <section className="panel empty-state">
            <Search aria-hidden="true" />
            <h2>No hay viajes para mostrar</h2>
            <p>Ajusta la busqueda o selecciona otro filtro operativo.</p>
          </section>
        )
      }
      rightPanel={
        <AlertsPanel
          trip={selectedTrip}
          onResolveAlert={(alertTitle) =>
            showToast(`Alerta revisada: ${alertTitle}`)
          }
        />
      }
    >
      {toast && (
        <div className="toast" role="status" aria-live="polite">
          <CheckCircle2 aria-hidden="true" />
          {toast}
        </div>
      )}
      <button
        className="mobile-sidebar-close"
        aria-label="Cerrar menu"
        onClick={() => setIsSidebarOpen(false)}
        hidden={!isSidebarOpen}
      >
        <X aria-hidden="true" />
      </button>
    </FleetOpsLayout>
  );
}

export default App;
