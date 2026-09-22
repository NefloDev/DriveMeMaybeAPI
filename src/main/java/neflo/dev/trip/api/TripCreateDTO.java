package neflo.dev.trip.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TripCreateDTO(
        UUID driver,
        LocalDate date,
        Integer durationMinutes,
        Integer distanceKm,
        String origin,
        String destination,
        String notes,
        List<UUID> passengers
) {
}
