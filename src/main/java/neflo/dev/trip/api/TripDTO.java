package neflo.dev.trip.api;

import java.time.LocalDate;
import java.util.UUID;

public record TripDTO(
        UUID id,
        UUID driverId,
        String driver,
        LocalDate date,
        Integer durationMinutes,
        Integer distanceKm,
        String origin,
        String destination,
        String notes
) {
}
