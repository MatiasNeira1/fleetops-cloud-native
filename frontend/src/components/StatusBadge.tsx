import type { AlertSeverity, TripStatus } from "../types/fleet";

interface StatusBadgeProps {
  label: TripStatus | AlertSeverity;
  tone?: "status" | "alert";
}

export function StatusBadge({ label, tone = "status" }: StatusBadgeProps) {
  const className = `status-badge ${tone}-${label.toLowerCase().replaceAll(" ", "-")}`;

  return (
    <span className={className}>
      <span className="status-dot" aria-hidden="true" />
      {label}
    </span>
  );
}
