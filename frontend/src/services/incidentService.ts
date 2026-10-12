import { mockTrips } from "../data/mockTrips";
import type { TripAlert } from "../types/fleet";

export async function getIncidentsByTrip(tripId: string): Promise<TripAlert[]> {
  return mockTrips.find((trip) => trip.id === tripId)?.alerts ?? [];
}
