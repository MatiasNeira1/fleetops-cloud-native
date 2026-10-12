import { mockTrips } from "../data/mockTrips";
import type { Trip } from "../types/fleet";

export async function getActiveTrips(): Promise<Trip[]> {
  return new Promise((resolve) => {
    window.setTimeout(() => resolve(mockTrips), 160);
  });
}

export async function getTripById(id: string): Promise<Trip | undefined> {
  const trips = await getActiveTrips();
  return trips.find((trip) => trip.id === id);
}
