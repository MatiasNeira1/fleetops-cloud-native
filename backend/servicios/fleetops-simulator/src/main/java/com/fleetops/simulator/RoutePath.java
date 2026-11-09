package com.fleetops.simulator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class RoutePath {
    public record Point(double latitude, double longitude) {
        public Point {
            if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException("Coordenadas invalidas");
            }
        }
    }

    private final List<Point> points;
    private final double[] cumulativeKm;

    public RoutePath(List<Point> points) {
        if (points == null || points.size() < 2) {
            throw new IllegalArgumentException("La ruta requiere al menos dos puntos");
        }
        this.points = List.copyOf(points);
        this.cumulativeKm = new double[points.size()];
        for (int index = 1; index < points.size(); index++) {
            cumulativeKm[index] = cumulativeKm[index - 1]
                + distanceKm(points.get(index - 1), points.get(index));
        }
        if (lengthKm() <= 0) {
            throw new IllegalArgumentException("La ruta no tiene distancia");
        }
    }

    public static RoutePath from(FlotaApi.Trip trip) {
        List<Point> points = new ArrayList<>();
        if (trip.routeCoordinates() != null) {
            for (List<BigDecimal> coordinate : trip.routeCoordinates()) {
                if (coordinate.size() != 2) throw new IllegalArgumentException("Ruta invalida");
                points.add(new Point(coordinate.get(0).doubleValue(), coordinate.get(1).doubleValue()));
            }
        }
        if (points.size() < 2) {
            points = List.of(new Point(trip.origin().latitude().doubleValue(),
                    trip.origin().longitude().doubleValue()),
                new Point(trip.destination().latitude().doubleValue(),
                    trip.destination().longitude().doubleValue()));
        }
        return new RoutePath(points);
    }

    public double lengthKm() { return cumulativeKm[cumulativeKm.length - 1]; }

    public Point at(double travelledKm) {
        double value = Math.max(0, Math.min(lengthKm(), travelledKm));
        for (int index = 1; index < points.size(); index++) {
            if (value <= cumulativeKm[index]) {
                double segment = cumulativeKm[index] - cumulativeKm[index - 1];
                double fraction = segment == 0 ? 1 : (value - cumulativeKm[index - 1]) / segment;
                Point from = points.get(index - 1);
                Point to = points.get(index);
                return new Point(from.latitude() + fraction * (to.latitude() - from.latitude()),
                    from.longitude() + fraction * (to.longitude() - from.longitude()));
            }
        }
        return points.getLast();
    }

    public double project(Point point) {
        double bestDistance = Double.MAX_VALUE;
        double bestAlong = 0;
        for (int index = 1; index < points.size(); index++) {
            Point a = points.get(index - 1);
            Point b = points.get(index);
            double latScale = 111.2;
            double lonScale = 111.2 * Math.cos(Math.toRadians(point.latitude()));
            double ax = a.longitude() * lonScale, ay = a.latitude() * latScale;
            double bx = b.longitude() * lonScale, by = b.latitude() * latScale;
            double px = point.longitude() * lonScale, py = point.latitude() * latScale;
            double dx = bx - ax, dy = by - ay;
            double fraction = (dx * dx + dy * dy) == 0 ? 0
                : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)));
            double distance = Math.hypot(px - (ax + fraction * dx), py - (ay + fraction * dy));
            if (distance < bestDistance) {
                bestDistance = distance;
                bestAlong = cumulativeKm[index - 1]
                    + fraction * (cumulativeKm[index] - cumulativeKm[index - 1]);
            }
        }
        return bestAlong;
    }

    private static double distanceKm(Point a, Point b) {
        double lat = Math.toRadians(b.latitude() - a.latitude());
        double lon = Math.toRadians(b.longitude() - a.longitude());
        double sinLat = Math.sin(lat / 2), sinLon = Math.sin(lon / 2);
        double h = sinLat * sinLat + Math.cos(Math.toRadians(a.latitude()))
            * Math.cos(Math.toRadians(b.latitude())) * sinLon * sinLon;
        return 6371.0088 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }
}
