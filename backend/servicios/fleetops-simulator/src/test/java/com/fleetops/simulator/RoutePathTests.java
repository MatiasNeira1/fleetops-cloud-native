package com.fleetops.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RoutePathTests {
    @Test
    void interpolatesAndProjectsAlongGeometry() {
        RoutePath path = new RoutePath(List.of(new RoutePath.Point(-33, -70),
            new RoutePath.Point(-33, -69.99), new RoutePath.Point(-32.99, -69.99)));
        RoutePath.Point middle = path.at(path.lengthKm() / 2);
        assertTrue(middle.latitude() >= -33 && middle.latitude() <= -32.99);
        assertTrue(middle.longitude() >= -70 && middle.longitude() <= -69.99);
        assertEquals(path.lengthKm() / 2, path.project(middle), 0.02);
        assertEquals(-32.99, path.at(path.lengthKm() + 100).latitude(), 0.000001);
    }
}
