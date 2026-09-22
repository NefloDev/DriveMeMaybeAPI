package neflo.dev.group.api;

import java.util.Map;

public record GroupInsights(
        int year,
        Integer month,
        Map<String, Integer> driverDrivingTime
) {
}
