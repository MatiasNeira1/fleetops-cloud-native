import type { TripTimelineItem } from "../types/fleet";

interface TripTimelineProps {
  timeline: TripTimelineItem[];
}

export function TripTimeline({ timeline }: TripTimelineProps) {
  return (
    <section className="timeline-card" aria-label="Linea de tiempo del viaje">
      {timeline.map((item) => (
        <div key={`${item.label}-${item.time}`} className={`timeline-step ${item.state}`}>
          <span className="timeline-dot" aria-hidden="true" />
          <strong>{item.label}</strong>
          <small>{item.time}</small>
        </div>
      ))}
    </section>
  );
}
