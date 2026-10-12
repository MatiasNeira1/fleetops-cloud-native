import type { TripStatusFilter } from "../types/fleet";

const filters: TripStatusFilter[] = ["Todos", "En ruta", "Con retraso", "Incidente"];

interface TripFiltersProps {
  activeFilter: TripStatusFilter;
  onFilterChange: (filter: TripStatusFilter) => void;
}

export function TripFilters({ activeFilter, onFilterChange }: TripFiltersProps) {
  return (
    <div className="filter-group" aria-label="Filtros de viaje">
      {filters.map((filter) => (
        <button
          key={filter}
          type="button"
          className={activeFilter === filter ? "filter-chip active" : "filter-chip"}
          onClick={() => onFilterChange(filter)}
        >
          {filter}
        </button>
      ))}
    </div>
  );
}
