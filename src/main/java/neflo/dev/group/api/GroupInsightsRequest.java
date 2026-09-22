package neflo.dev.group.api;


public record GroupInsightsRequest(
        GroupInsightsPeriodTypes periodType,
        Integer year,
        Integer month
) {
}
