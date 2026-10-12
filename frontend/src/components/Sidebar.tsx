import { AlertTriangle, Boxes, Home, MapPinned, Truck, X } from "lucide-react";

const navItems = [
  { label: "Inicio", icon: Home },
  { label: "Flota", icon: Truck },
  { label: "Viajes", icon: MapPinned },
  { label: "Entregas", icon: Boxes },
  { label: "Incidentes", icon: AlertTriangle },
];

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  return (
    <aside className={`sidebar ${isOpen ? "is-open" : ""}`} aria-label="Navegacion principal">
      <div className="sidebar-brand">
        <span className="brand-mark">F</span>
        <span>FleetOps</span>
        <button className="icon-button sidebar-close" type="button" aria-label="Cerrar navegacion" onClick={onClose}>
          <X aria-hidden="true" />
        </button>
      </div>
      <nav>
        <ul className="nav-list">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = item.label === "Viajes";

            return (
              <li key={item.label}>
                <a className={isActive ? "nav-link active" : "nav-link"} href={`#${item.label.toLowerCase()}`}>
                  <Icon aria-hidden="true" />
                  <span>{item.label}</span>
                  {isActive && <span className="active-text">Activo</span>}
                </a>
              </li>
            );
          })}
        </ul>
      </nav>
      <div className="sidebar-footer">
        <p>Centro de operaciones</p>
        <strong>Zona Chile</strong>
      </div>
    </aside>
  );
}
