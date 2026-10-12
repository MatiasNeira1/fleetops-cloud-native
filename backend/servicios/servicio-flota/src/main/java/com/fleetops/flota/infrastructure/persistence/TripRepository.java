package com.fleetops.flota.infrastructure.persistence;

import com.fleetops.flota.domain.Trip;
import com.fleetops.flota.domain.TripStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, String> {
    List<Trip> findByStatusNotIn(Collection<TripStatus> statuses);
    List<Trip> findByStatus(TripStatus status);
}
