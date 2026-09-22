package neflo.dev.trip.infrastructure.persistence;

import neflo.dev.trip.domain.TripModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<TripModel, UUID> {
}
