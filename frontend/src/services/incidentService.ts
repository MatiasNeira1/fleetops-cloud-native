import { getAlertsByTripId } from "./tripService";
import type { TripAlert } from "../types/fleet";

export async function getIncidentsByTrip(tripId: string): Promise<TripAlert[]> {
  return getAlertsByTripId(tripId);
}
