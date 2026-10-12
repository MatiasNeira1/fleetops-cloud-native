import { Bell, ChevronDown, Menu, Search } from "lucide-react";

interface TopBarProps {
  query: string;
  onQueryChange: (query: string) => void;
  onMenuClick: () => void;
}

export function TopBar({ query, onQueryChange, onMenuClick }: TopBarProps) {
  return (
    <header className="topbar">
      <button
        className="icon-button mobile-menu"
        type="button"
        aria-label="Abrir navegacion"
        onClick={onMenuClick}
      >
        <Menu aria-hidden="true" />
      </button>
      <div className="brand-inline" aria-label="FleetOps">
        <span className="brand-mark">F</span>
        <span>FleetOps</span>
      </div>
      <label className="global-search">
        <Search aria-hidden="true" />
        <span className="sr-only">Buscar viaje, vehiculo o conductor</span>
        <input
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
          placeholder="Buscar viaje, vehiculo o conductor"
          type="search"
        />
      </label>
      <div className="topbar-actions">
        <button className="icon-button notification-button" type="button" aria-label="Notificaciones">
          <Bell aria-hidden="true" />
          <span aria-hidden="true" className="notification-dot" />
        </button>
        <button className="user-menu" type="button" aria-label="Abrir menu de usuario">
          <span className="avatar" aria-hidden="true">MR</span>
          <span className="user-meta">
            <strong>Maria Ramos</strong>
            <small>Operaciones</small>
          </span>
          <ChevronDown aria-hidden="true" />
        </button>
      </div>
    </header>
  );
}
