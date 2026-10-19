import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, Search, X } from "lucide-react";
import { InteractionStatus } from "@azure/msal-browser";
import { useMsal } from "@azure/msal-react";
import { authSettings } from "./auth/config";
import { LoginScreen } from "./auth/LoginScreen";
import { FleetOpsLayout } from "./components/FleetOpsLayout";
import { TopBar } from "./components/TopBar";
import { Sidebar } from "./components/Sidebar";
import { TripFilters } from "./components/TripFilters";
import { TripList } from "./components/TripList";
import { TripDetails } from "./components/TripDetails";
import { AlertsPanel } from "./components/AlertsPanel";
import {
  createIncidentForTrip,
  getActiveTrips,
  resolveAlert,
  sendTelemetryForTrip,
} from "./services/tripService";
import type { Trip, TripStatusFilter } from "./types/fleet";

function App() {
  const { instance, accounts, inProgress } = useMsal();
  if (inProgress !== InteractionStatus.None) {
    return <main className="login-page"><p role="status">Comprobando sesión…</p></main>;
  }
  const account = instance.getActiveAccount() ?? accounts[0];
  return account ? <Dashboard username={account.name ?? account.username} onLogout={() => instance.logoutRedirect({ account })} onRenew={() => instance.acquireTokenRedirect({ ...authSettings.loginRequest, account })} /> : <LoginScreen />;
}

interface DashboardProps {
  username: string;
  onLogout: () => Promise<void>;
  onRenew: () => Promise<void>;
}

function Dashboard({ username, onLogout, onRenew }: DashboardProps) {
  const [trips, setTrips] = useState<Trip[]>([]);
  const [selectedTripId, setSelectedTripId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<TripStatusFilter>("Todos");
  const [query, setQuery] = useState("");
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadTrips = async (preserveSelection = true) => {
    setIsLoading(true);
    setError(null);
    try {
      const activeTrips = await getActiveTrips();
      setTrips(activeTrips);
      setSelectedTripId((current) => {
        if (preserveSelection && current && activeTrips.some((trip) => trip.id === current)) {
          return current;
        }
        return activeTrips[0]?.id ?? null;
      });
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "No fue posible cargar los viajes activos.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    let isMounted = true;

    async function loadInitialTrips() {
      try {
        const activeTrips = await getActiveTrips();
        if (!isMounted) {
          return;
        }
        setTrips(activeTrips);
        setSelectedTripId(activeTrips[0]?.id ?? null);
      } catch (caught) {
        if (!isMounted) {
          return;
        }
        setError(caught instanceof Error ? caught.message : "No fue posible cargar los viajes activos.");
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadInitialTrips();

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

  const runAction = async (successMessage: string, action: () => Promise<void>) => {
    try {
      await action();
      await loadTrips();
      showToast(successMessage);
    } catch (caught) {
      showToast(caught instanceof Error ? caught.message : "No fue posible completar la accion.");
    }
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
          username={username}
          onLogout={() => void onLogout()}
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
          {error?.includes("Renovar sesión") && <button className="secondary-button" type="button" onClick={() => void onRenew()}>Renovar sesión</button>}
        </section>
      }
      mainPanel={
        selectedTrip ? (
          <TripDetails
            trip={selectedTrip}
            onRefresh={() => {
              void loadTrips();
              showToast("Datos actualizados.");
            }}
            onSendTelemetry={(trip) =>
              void runAction("Telemetria enviada.", () => sendTelemetryForTrip(trip))
            }
            onReportIncident={(trip) =>
              void runAction("Incidente reportado.", () => createIncidentForTrip(trip))
            }
          />
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
          onResolveAlert={(alert) =>
            void runAction(`Alerta resuelta: ${alert.title}`, () => resolveAlert(alert.id))
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
